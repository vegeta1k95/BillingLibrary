package com.sdk.billinglibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;
import com.sdk.billinglibrary.interfaces.ISubscriptionListener;
import com.sdk.billinglibrary.interfaces.ISkuListener;
import com.sdk.billinglibrary.onboard.OnBoardActivity;

import java.util.ArrayList;
import java.util.List;

public class BillingManager {

    static final String LOG_TAG = "MYTAG";

    public static void addOnBoardLayout(String layoutName) {
        OnBoardActivity.onBoardLayouts.add(layoutName);
    }

    public static void addBillingLayout(String layoutName) {
        BillingActivity.billingLayouts.add(layoutName);
    }

    public static void init(Context context) {
        LocalConfig.init(context);
        if (manager == null)
            manager = new BillingManager(context.getApplicationContext());
    }

    static String DIALOG_EXIT_ICONS;

    private static Handler handler = new Handler(Looper.getMainLooper());
    private static BillingManager manager;
    private static boolean TEST = false;

    public static void setDialogExitIcons(String layout) { DIALOG_EXIT_ICONS = layout; }
    public static void setTestMode(boolean test) { TEST = test; }

    public static void setTimeProposed() { LocalConfig.setTimeProposed(); }
    public static boolean isTimeToPropose() { return LocalConfig.isTimeToPropose(); }

    public static BillingManager get(Context context) {
        init(context);
        return manager;
    }

    private BillingClient mBillingClient;
    private IOnPurchaseListener mOnPurchaseListener;

    private BillingManager(Context context) {
        mBillingClient = BillingClient
                .newBuilder(context)
                .setListener((billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && list != null) {

                        boolean purchased = false;

                        for (Purchase purchase : list) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                if (!purchase.isAcknowledged())
                                    acknowledgePurchase(purchase);
                                LocalConfig.subscribeLocally(true);
                                purchased = true;
                            }
                        }

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

                })
                .enablePendingPurchases()
                .build();
    }

    public void startOnBoard(Context context) {
        if (context == null)
            return;
        if (!LocalConfig.isOnBoardShown()) {
            Intent intent = new Intent(context, OnBoardActivity.class);
            intent.putExtra(OnBoardActivity.INTENT_ONLY_ONBOARD, true);
            context.startActivity(intent);
        }
    }

    public void startActivity(Context context) {
        if (context == null)
            return;
        if (LocalConfig.isOnBoardShown()) {
            Intent intent = new Intent(context, BillingActivity.class);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, OnBoardActivity.class);
            context.startActivity(intent);
        }
    }

    public void requestSubscription(ISubscriptionListener callback) {

        if (TEST) {
            LocalConfig.subscribeLocally(true);
            callback.onResult(true);
            return;
        }

        updatePurchases(new ISubscriptionListener() {
            @Override
            public void onResult(boolean isSubscribed) {
                handler.post(()->callback.onResult(isSubscribed));
            }

            @Override
            public void onFailed(String error) {
                handler.post(()->callback.onFailed(error));
            }
        });
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        mBillingClient.acknowledgePurchase(params, result -> {});
    }

    private boolean isSubscriptionSupported() {
        return mBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
                .getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    private interface IConnectionCallback {
        void onConnectSuccess();
        void onConnectFailed(int error);
    }

    private void startConnection(IConnectionCallback callback) {
        if (mBillingClient.isReady()) {
            callback.onConnectSuccess();
        } else {
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d(LOG_TAG, "Billing setup finished!");
                        callback.onConnectSuccess();
                    } else {
                        callback.onConnectFailed(billingResult.getResponseCode());
                    }
                }

                @Override
                public void onBillingServiceDisconnected() { /* ... */ }
            });
        }
    }

    private void updatePurchases(ISubscriptionListener callback) {
        startConnection(new IConnectionCallback() {
            @Override
            public void onConnectSuccess() {

                if (!isSubscriptionSupported()) {
                    Log.d(LOG_TAG, "Subscription are not supported!");
                    LocalConfig.subscribeLocally(true);
                    callback.onResult(true);
                    return;
                }

                mBillingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, (result, purchases) -> {
                    if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        callback.onFailed("Error code: " + result.getResponseCode());
                        return;
                    }
                    boolean subscribed = false;
                    for (Purchase purchase : purchases) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            if (!purchase.isAcknowledged())
                                acknowledgePurchase(purchase);
                            subscribed = true;
                        }
                    }
                    LocalConfig.subscribeLocally(subscribed);
                    callback.onResult(subscribed);
                });
            }

            @Override
            public void onConnectFailed(int error) {
                callback.onFailed("Connection failed: " + error);
            }
        });
    }

    void retrieveSubs(String trialSubId, String premiumSubId, ISkuListener listener) {
        if (trialSubId == null
                || trialSubId.isEmpty()
                || premiumSubId == null
                || premiumSubId.isEmpty()) {
            listener.onFailed();
            return;
        }

        startConnection(new IConnectionCallback() {
            @Override
            public void onConnectSuccess() {
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

            @Override
            public void onConnectFailed(int error) {
                listener.onFailed();
            }
        });

    }

    void launchPurchaseFlow(Activity activity, SkuDetails sub, IOnPurchaseListener listener) {
        mOnPurchaseListener = listener;
        startConnection(new IConnectionCallback() {
            @Override
            public void onConnectSuccess() {

                if (!isSubscriptionSupported()) {
                    Log.d(LOG_TAG, "Subscriptions are not supported!");
                    LocalConfig.subscribeLocally(true);
                    if (mOnPurchaseListener != null)
                        mOnPurchaseListener.onPurchaseDone();
                    return;
                }

                if (sub == null)
                    return;

                try {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(sub).build();
                    mBillingClient.launchBillingFlow(activity, flowParams);
                } catch (IllegalArgumentException e) {
                    mOnPurchaseListener.onError();
                }
            }

            @Override
            public void onConnectFailed(int error) {
                if (mOnPurchaseListener != null) {
                    mOnPurchaseListener.onError();
                }
            }

        });
    }
}
