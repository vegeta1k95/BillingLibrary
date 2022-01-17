package com.sdk.billinglibrary;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;


class RemoteConfig {

    private static final String SUB_TRIAL = "sub_trial";
    private static final String SUB_PREMIUM = "sub_premium";

    public interface IOnFetchSubsListener {
        void onComplete(String subTrial, String subPremium);
    }

    static void fetchSubs(Context context, IOnFetchSubsListener listener) {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        Map<String, Object> defaults = new HashMap<>();

        TypedValue trial = new TypedValue();
        TypedValue premium = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.billing_default_premium, premium, true);
        context.getTheme().resolveAttribute(R.attr.billing_default_trial, trial, true);
        defaults.put(SUB_TRIAL, trial.coerceToString());
        defaults.put(SUB_PREMIUM, premium.coerceToString());
        config.setDefaultsAsync(defaults).addOnCompleteListener(t ->
                config.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(BillingManager.LOG_TAG, "Fetched new config!");
            } else {
                Log.d(BillingManager.LOG_TAG, "Fetch failed!");
            }

            String subTrial = config.getString(SUB_TRIAL);
            String subPremium = config.getString(SUB_PREMIUM);

            Log.d(BillingManager.LOG_TAG, "Trial: " + subTrial);
            Log.d(BillingManager.LOG_TAG, "Premium: " + subPremium);

            listener.onComplete(subTrial, subPremium);
        }));
    }
}
