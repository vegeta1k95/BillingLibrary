package com.sdk.billinglibrary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

public class RateUs {

    private static final String PREFERENCES = "rate_us";
    private static final String RATE_US = "RATE_US";
    private static final String LAST_REQUESTED = "RATE_US_LAST";

    private static final String MARKET_APP_URL = "market://details?id=%s";
    private static final String MARKET_WEB_URL = "https://play.google.com/store/apps/details?id=%s";

    private static boolean isTimeToRequest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        long now = System.currentTimeMillis();
        long last = prefs.getLong(LAST_REQUESTED, 0);

        if (last == 0) {
            try {
                last = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException e) {
                last = now;
            }
        }

        return (int) ((now - last) / (1000 * 60 * 60 * 24)) >= 2;
    }

    private static void setLastRequested(Context context, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_REQUESTED, timestamp);
        editor.apply();
    }

    private static void setRating(Context context, float rating) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(RATE_US, rating);
        editor.apply();
    }

    private static boolean isRated(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        return prefs.contains(RATE_US);
    }

    public static void showDialog(final Activity activity, boolean finish, boolean ignoreCheck) {

        if (!ignoreCheck && (isRated(activity) || !isTimeToRequest(activity))) {
            if (finish)
                activity.finish();
            return;
        }

        setLastRequested(activity, System.currentTimeMillis());

        final AlertDialog dialog = new AlertDialog.Builder(activity).create();
        final View layout = LayoutInflater.from(activity).inflate(R.layout.rate_us,
                null, false);

        /* Set transparent background */
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView rate = layout.findViewById(R.id.rate_btn_rate);
        TextView later = layout.findViewById(R.id.rate_btn_later);

        final String market_app = String.format(MARKET_APP_URL, activity.getPackageName());
        final String market_web = String.format(MARKET_WEB_URL, activity.getPackageName());

        later.setOnClickListener(v -> {
            dialog.dismiss();
            if (finish)
                activity.finish();
        });

        rate.setOnClickListener(v -> {
            RatingBar bar = layout.findViewById(R.id.rate_bar);
            if (bar.getRating() > 3F) {
                setRating(activity, bar.getRating());

                ReviewManager manager = ReviewManagerFactory.create(activity);
                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ReviewInfo reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                        flow.addOnCompleteListener(t -> {
                            Toast.makeText(activity, R.string.rate_us_thanks, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            if (finish)
                                activity.finish();
                        });
                    } else {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(market_app));
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException exc) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(market_web));
                            activity.startActivity(intent);
                        }

                        dialog.dismiss();
                        if (finish)
                            activity.finish();
                    }
                });

            } else {
                Toast.makeText(activity, R.string.rate_us_thanks, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (finish)
                    activity.finish();
            }
        });

        dialog.setView(layout);
        dialog.setCancelable(false);
        dialog.show();

    }

}

