package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;
import com.sdk.billinglibrary.interfaces.ISubsListener;

import java.util.ArrayList;
import java.util.List;

class BillingManager implements BillingClientStateListener,
        PurchasesUpdatedListener, PurchasesResponseListener,
        DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    static final String LOG_TAG = "MYTAG (Billing)";

    private static BillingManager mManager;

    static void initialize(@NonNull Application application, IOnInitializationComplete listener) {
        LocalConfig.init(application.getApplicationContext());
        if (mManager == null) {
            mManager = new BillingManager(application, listener);
            mManager.restart();
        }
    }

    static BillingManager getInstance() {
        return mManager;
    }

    private final Application mApplication;
    private Activity mCurrentActivity;

    private BillingClient mBillingClient;
    private IOnPurchaseListener mOnPurchaseListener;
    private final IOnInitializationComplete mOnInitializationListener;
    private Runnable mRunnableConsumable;

    private BillingManager(@NonNull Application application, IOnInitializationComplete listener) {
        mApplication = application;
        mApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        mOnInitializationListener = listener;
    }

    void restart() {
        if (mBillingClient != null) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
        mBillingClient = BillingClient
                .newBuilder(mApplication)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        mBillingClient.startConnection(this);
    }

    private void executeListeners() {
        mOnInitializationListener.onComplete();
        if (mRunnableConsumable != null) {
            mRunnableConsumable.run();
            mRunnableConsumable = null;
        }
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
                executeListeners();
            }

        } else {
            Log.d(LOG_TAG, "Billing setup failed: "
                    + billingResult.getResponseCode() + " | "
                    + billingResult.getDebugMessage());
            executeListeners();
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
        executeListeners();
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
        mBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                this);
    }

    void retrieveSubs(@NonNull List<String> subIds, ISubsListener listener) {

        if (subIds.isEmpty()) {
            listener.onResult(false, null);
            return;
        }

        Runnable runnable = () -> {
            List<QueryProductDetailsParams.Product> products = new ArrayList<>();
            QueryProductDetailsParams.Product.Builder builder = QueryProductDetailsParams.Product.newBuilder();

            for (String subId : subIds) {
                products.add(builder.setProductId(subId).setProductType(BillingClient.ProductType.SUBS).build());
            }

            QueryProductDetailsParams queryProductDetailsParams =
                    QueryProductDetailsParams.newBuilder()
                            .setProductList(products)
                            .build();

            mBillingClient.queryProductDetailsAsync(queryProductDetailsParams,
                    (billingResult, productDetailsList) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            if (productDetailsList.size() != subIds.size()) {
                                Log.d(LOG_TAG, "Fetched too few SUBS (" +
                                        productDetailsList.size() + "), should be: " + subIds.size());
                                listener.onResult(false, null);
                            } else {
                                listener.onResult(true,productDetailsList);
                            }
                        } else {
                            listener.onResult(false, null);
                        }
            });
        };

        if (mBillingClient.isReady())
            runnable.run();
        else
            mRunnableConsumable = runnable;
    }

    void launchPurchaseFlow(Activity activity, ProductDetails product, String token, IOnPurchaseListener listener) {
        mOnPurchaseListener = listener;
        if (!mBillingClient.isReady() || product == null) {
            if (mOnPurchaseListener != null) {
                mOnPurchaseListener.onError();
            }
            return;
        }

        try {
            List<BillingFlowParams.ProductDetailsParams> params = new ArrayList<>();
            params.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .setOfferToken(token)
                    .build());
            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(params).build();
            mBillingClient.launchBillingFlow(activity, flowParams);
        } catch (IllegalArgumentException e) {
            mOnPurchaseListener.onError();
        }

    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) { mCurrentActivity = activity; }
    @Override public void onActivityResumed(@NonNull Activity activity) { mCurrentActivity = activity; }
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) { mCurrentActivity = null; }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mCurrentActivity != null
                && mCurrentActivity.getPackageName().equals(mApplication.getPackageName())
                && !(mCurrentActivity instanceof BillingActivity)
                && !(mCurrentActivity instanceof BillingOfferActivity)
                && !Billing.isSubscribed()
                && !LocalConfig.isFirstTimeBilling()
                && !Billing.isLaunchedFromPush(mCurrentActivity)) {
            Billing.startBillingActivity(mCurrentActivity, true);
        }
    }
}