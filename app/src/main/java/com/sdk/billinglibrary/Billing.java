package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sdk.billinglibrary.interfaces.IOnInitializationComplete;

public class Billing implements Application.ActivityLifecycleCallbacks {

    private final boolean mTestMode;
    private final Application mApplication;
    private Activity mActivity;

    public Billing(Application application, boolean testMode,
                   @Nullable IOnInitializationComplete listener) {
        mTestMode = testMode;
        mApplication = application;
        BillingManager.initialize(application.getApplicationContext(), () -> {
            mApplication.registerActivityLifecycleCallbacks(this);
            if (listener != null)
                listener.onComplete();
        });
    }

    public boolean isSubscribed() {
        if (mTestMode)
            return true;
        return LocalConfig.isSubscribedLocally();
    }

    public void startBillingActivity() {
        if (mActivity == null)
            return;
        Intent intent = new Intent(mActivity, BillingActivity.class);
        mActivity.startActivity(intent);
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {
        mActivity = activity;
        if (!isSubscribed() || LocalConfig.isTimeToPropose()) {
            startBillingActivity();
            LocalConfig.setTimeProposed();
        }
    }
    @Override public void onActivityResumed(@NonNull Activity activity) { mActivity = activity; }
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) { mActivity = null; }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) { mActivity = null; }

}
