package com.sdk.billinglibrary;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

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

    private static boolean mTestMode;

    public static void initialize(Application application, boolean testMode,
                                  @Nullable IOnInitializationComplete listener) {
        mTestMode = testMode;
        BillingManager.initialize(application, () -> {
            if (listener != null)
                listener.onComplete();
        });
    }

    public static class OfferWorker extends Worker {

        private static final String CHANNEL_ID = "billing_offer_channel";
        private static final Integer NOTIFICATION_ID = 1440;

        private Context mContext;

        public OfferWorker(
                @NonNull Context context,
                @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
            mContext = context;
        }

        @NonNull
        @Override
        public Result doWork() {
            if (!Billing.isSubscribed())
                createNotification();
            return Result.success();
        }

        private PendingIntent createPendingIntent() {
            Intent intent = new Intent(mContext, BillingOfferActivity.class);
            return PendingIntent.getActivity(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        private void createNotification() {

            NotificationManager nm = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    channel = new NotificationChannel(CHANNEL_ID, "Subscription Offer",
                            NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(channel);
                }
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                    getApplicationContext(), CHANNEL_ID);
            mBuilder.setSmallIcon(R.drawable.ic_billing_push)
                    .setContentTitle("Special Offer")
                    .setAutoCancel(true)
                    .setContentText("Special Offer just for You! Subscribe now!")
                    .setContentIntent(createPendingIntent())
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBuilder.setChannelId(CHANNEL_ID);
            }

            nm.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    public static void startOfferActivityIfNeeded(Activity activity) {

        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();

        if (LocalConfig.isFirstTimeOffer()) {
            WorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(OfferWorker.class)
                    .setInitialDelay(1, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(activity).enqueue(uploadWorkRequest);
        } else if (extras != null) {
            if (extras.containsKey("billing_offer_data")
                    && !isSubscribed()
                    && LocalConfig.daysPassedSinceFirstLaunch(1)) {
                activity.startActivity(new Intent(activity, BillingOfferActivity.class));
            }
        }
    }

    public static boolean isSubscribed() {
        if (mTestMode)
            return true;
        return LocalConfig.isSubscribedLocally();
    }

    public static void startBillingActivity(Activity activity) {
        startBillingActivity(activity, false);
    }

    public static void startBillingActivity(Activity activity, boolean doChecks) {

        if (doChecks) {
            if (isSubscribed() || !LocalConfig.isTimeToPropose())
                return;
            LocalConfig.setTimeProposed();
        }

        if (activity == null)
            return;

        Intent intent = new Intent(activity, BillingActivity.class);
        activity.startActivity(intent);
    }
}
