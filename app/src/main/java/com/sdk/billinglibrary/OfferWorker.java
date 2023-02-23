package com.sdk.billinglibrary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class OfferWorker extends Worker {

    private static final String CHANNEL_ID = "billing_offer_channel";
    private static final Integer NOTIFICATION_ID = 1440;

    private final Context mContext;

    public OfferWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Billing.getStatus() == Billing.Status.NOT_SUBSCRIBED)
            createNotification();
        return Result.success();
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(mContext, BillingOfferActivity.class);
        intent.putExtra("billing_push_offer", true);
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
                .setContentTitle(mContext.getString(R.string.offer_notification_title))
                .setContentText(mContext.getString(R.string.offer_notificatoin_body))
                .setAutoCancel(true)
                .setContentIntent(createPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setChannelId(CHANNEL_ID);
        }

        nm.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
