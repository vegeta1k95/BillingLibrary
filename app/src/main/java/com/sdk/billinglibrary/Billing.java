package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;

public class Billing {

    private static boolean mTestMode;

    public static void initialize(Application application, boolean testMode,
                                  @Nullable IOnInitializationComplete listener) {
        mTestMode = testMode;
        BillingManager.initialize(application.getApplicationContext(), () -> {
            if (listener != null)
                listener.onComplete();
        });
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
