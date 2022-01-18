package com.sdk.billinglibrary;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;
import com.sdk.billinglibrary.interfaces.ISkuListener;

import java.util.ArrayList;
import java.util.List;

class BillingManager implements BillingClientStateListener,
        PurchasesUpdatedListener, PurchasesResponseListener {

    static final String LOG_TAG = "MYTAG (Billing)";

    private static Context mContext;
    private static BillingManager mManager;

    static void initialize(@NonNull Context context, IOnInitializationComplete listener) {
        LocalConfig.init(context);
        mContext = context.getApplicationContext();
        if (mManager == null)
            mManager = new BillingManager(listener);
    }

    static BillingManager getInstance() {
        return mManager;
    }

    private BillingClient mBillingClient;
    private IOnPurchaseListener mOnPurchaseListener;
    private IOnInitializationComplete mOnInitializationListener;

    private BillingManager(IOnInitializationComplete listener) {
        mOnInitializationListener = listener;
        restart();
    }

    private void restart() {
        if (mBillingClient != null) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
        mBillingClient = BillingClient
                .newBuilder(mContext)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        mBillingClient.startConnection(this);
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            Log.d(LOG_TAG, "Billing setup finished!");

            boolean isSubSupported = isSubscriptionSupported();

            if (isSubSupported) {
                Log.d(LOG_TAG,"Subscriptions are supported!");
                queryPurchases();
            } else {
                Log.d(LOG_TAG, "Subscription are not supported!");
                LocalConfig.subscribeLocally(true);
                mOnInitializationListener.onComplete();
            }

        } else {
            Log.d(LOG_TAG, "Billing setup failed: "
                    + billingResult.getResponseCode() + " | "
                    + billingResult.getDebugMessage());
            mOnInitializationListener.onComplete();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        restart();
    }

    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            acknowledgePurchases(list);
            Log.d(LOG_TAG, "Purchases are acknowledged!");
        } else {
            Log.d(LOG_TAG, "Failed to query purchases: "
                    + billingResult.getResponseCode() + " | "
                    + billingResult.getDebugMessage());
        }
        mOnInitializationListener.onComplete();
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && list != null) {

            boolean purchased = acknowledgePurchases(list);

            if (mOnPurchaseListener == null)
                return;

            if (purchased)
                mOnPurchaseListener.onPurchaseDone();
            else
                mOnPurchaseListener.onPurchaseCancelled();

        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (mOnPurchaseListener != null)
                mOnPurchaseListener.onPurchaseCancelled();
        } else {
            if (mOnPurchaseListener != null)
                mOnPurchaseListener.onPurchaseFail();
        }
    }

    private boolean acknowledgePurchases(List<Purchase> list) {

        boolean purchased = false;

        for (Purchase purchase : list) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();
                    mBillingClient.acknowledgePurchase(params, result -> {});
                }
                purchased = true;
            }
        }
        LocalConfig.subscribeLocally(purchased);
        return purchased;
    }

    private boolean isSubscriptionSupported() {
        return mBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                .getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    void queryPurchases() {
        mBillingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, this);
    }

    void retrieveSubs(String trialSubId, String premiumSubId, ISkuListener listener) {

        if (trialSubId == null
                || trialSubId.isEmpty()
                || premiumSubId == null
                || premiumSubId.isEmpty()
                || !mBillingClient.isReady()) {
            listener.onFailed();
            return;
        }

        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();

        List<String> subs = new ArrayList<>();
        subs.add(trialSubId);
        subs.add(premiumSubId);

        params.setSkusList(subs).setType(BillingClient.SkuType.SUBS);
        mBillingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                        if (skuDetailsList == null) {
                            listener.onFailed();
                            return;
                        }

                        if (skuDetailsList.size() != 2) {
                            Log.d(LOG_TAG, "Fetched too few SKUs, should be 2!");
                            listener.onFailed();
                        } else {

                            SkuDetails trial = null;
                            SkuDetails full = null;

                            for (SkuDetails sku : skuDetailsList) {
                                if (sku.getSku().equals(trialSubId))
                                    trial = sku;
                                else if (sku.getSku().equals(premiumSubId))
                                    full = sku;
                            }

                            if (trial != null && full != null)
                                listener.onResult(trial, full);
                            else
                                listener.onFailed();
                        }

                    } else {
                        listener.onFailed();
                    }
                });
    }

    void launchPurchaseFlow(Activity activity, SkuDetails sub, IOnPurchaseListener listener) {
        mOnPurchaseListener = listener;

        if (!mBillingClient.isReady() || sub == null) {
            if (mOnPurchaseListener != null) {
                mOnPurchaseListener.onError();
            }
            return;
        }

        try {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(sub).build();
            mBillingClient.launchBillingFlow(activity, flowParams);
        } catch (IllegalArgumentException e) {
            mOnPurchaseListener.onError();
        }

    }

}
