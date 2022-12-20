package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;

import java.util.concurrent.TimeUnit;

public class Billing {

    public interface ICallback {
        void onDismiss();
    }

    static boolean mTestMode;
    static ICallback mCallback;

    public static void initialize(Application application, boolean testMode,
                                  @Nullable IOnInitializationComplete listener) {
        mTestMode = testMode;
        BillingManager.initialize(application, () -> {
            if (listener != null)
                listener.onComplete();
        });
    }

    public static boolean isSubscribed() {
        if (mTestMode)
            return true;
        return LocalConfig.isSubscribedLocally();
    }

    public static void startOfferActivityIfNeeded(Activity activity, long delay) {

        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();

        if (LocalConfig.isFirstTimeOffer()) {
            WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(OfferWorker.class)
                    .setInitialDelay(delay, TimeUnit.MINUTES)
                    .build();
            WorkManager.getInstance(activity).enqueue(uploadWorkRequest);
        } else if (extras != null) {
            if (extras.containsKey("billing_push_offer")
                    && !isSubscribed()
                    && LocalConfig.daysPassedSinceFirstOffer(1)) {
                activity.startActivity(new Intent(activity, BillingOfferActivity.class));
            }
        }
    }

    public static void startBillingActivity(Activity activity) {
        startBillingActivity(activity, null);
    }

    public static void startBillingActivity(Activity activity, ICallback callback) {

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
