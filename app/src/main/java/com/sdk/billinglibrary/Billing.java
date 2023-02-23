package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ktx.Firebase;
import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;

import java.util.concurrent.TimeUnit;

public class Billing {

    public enum Status {
        SUBSCRIBED,
        NOT_SUBSCRIBED,
        UNSUPPORTED
    }

    public static final String TEST_MODE = "TEST_MODE";
    public static final String UNSUPPORTED = "NOT_SUPPORTED";

    public interface ICallback {
        void onDismiss();
    }

    static boolean mTestMode;
    static ICallback mCallback;

    public static void initialize(Application application, boolean testMode,
                                  @Nullable IOnInitializationComplete listener) {
        mTestMode = testMode;

        Context context = application.getApplicationContext();

        FirebaseApp.initializeApp(context);

        // Init shared preferences
        LocalConfig.init(context);

        // Request Firebase Remote Config
        RemoteConfig.fetchSubs(context, (isSuccessful -> {

            // Get fetched sub IDs
            String trialSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_TRIAL);
            String premiumSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_PREMIUM);

            // If one of fetched sub IDs is "NOT_SUPPORTED" we consider user "subscribed"
            if (trialSubId.equals(UNSUPPORTED) || premiumSubId.equals(UNSUPPORTED)) {

                // Denote billing as unsupported only if there is no currently active subscription
                // (to avoid currently subscribed users receive ads, etc)
                if (LocalConfig.getCurrentSubscription() == null)
                    LocalConfig.subscribeLocally(UNSUPPORTED);

                // Launch callback, if any
                if (listener != null)
                    listener.onComplete();

                // Do not init billing in this case - unnecessary.
                return;
            }

            // Otherwise initialize billing
            BillingManager.initialize(application, () -> {

                // Launch callback, if any
                if (listener != null)
                    listener.onComplete();
            });

        }));
    }

    public static Status getStatus() {
        if (mTestMode)
            return Status.SUBSCRIBED;

        String sub = LocalConfig.getCurrentSubscription();

        if (sub == null || sub.isEmpty())
            return Status.NOT_SUBSCRIBED;
        else if (sub.equals(UNSUPPORTED))
            return Status.UNSUPPORTED;
        else
            return Status.SUBSCRIBED;
    }

    public static void manageSubs(Activity activity) {
        String url = "https://play.google.com/store/account/subscriptions";
        String sub = LocalConfig.getCurrentSubscription();
        if (sub != null && !mTestMode && !sub.equals(UNSUPPORTED)) {
            url += "?sku=" + sub + "&package=" + activity.getPackageName();
        }
        Uri page = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, page);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }

    public static void startOfferActivityIfNeeded(@Nullable Activity activity, long delay) {

        if (activity == null)
            return;

        if (getStatus() != Status.NOT_SUBSCRIBED)
            return;

        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();

        if (LocalConfig.isFirstTimeOffer()) {
            WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(OfferWorker.class)
                    .setInitialDelay(delay, TimeUnit.MINUTES)
                    .build();
            WorkManager.getInstance(activity).enqueue(uploadWorkRequest);
        } else if (extras != null) {
            if (extras.containsKey("billing_push_offer")
                    && LocalConfig.daysPassedSinceFirstOffer(1)) {
                activity.startActivity(new Intent(activity, BillingOfferActivity.class));
            }
        }
    }

    public static void startBillingActivity(@Nullable Activity activity) {
        startBillingActivity(activity, null);
    }

    public static void startBillingActivity(@Nullable Activity activity, ICallback callback) {

        mCallback = callback;

        if (activity == null) {
            if (callback != null)
                callback.onDismiss();
            return;
        }

        Intent intent = new Intent(activity, BillingActivity.class);
        activity.startActivity(intent);
    }

}
