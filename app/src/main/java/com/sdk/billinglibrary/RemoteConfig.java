package com.sdk.billinglibrary;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;

import java.util.HashMap;
import java.util.Map;


class RemoteConfig {

    static final String SUB_TRIAL = "sub_trial";
    static final String SUB_PREMIUM = "sub_premium";

    static final String SUB_OFFER_WEEKLY = "sub_offer_weekly";
    static final String SUB_OFFER_TRIAL = "sub_offer_trial";
    static final String SUB_OFFER_LIFETIME = "sub_offer_lifetime";

    static final String DEFAULT_WEEKLY = "weeklyacessspecialoffer";
    static final String DEFAULT_TRIAL = "weeklytrialspecialoffer";
    static final String DEFAULT_LIFETIME = "lifetimespecialoffer";

    public interface IOnFetchSubsListener {
        void onComplete(boolean isSuccessful);
    }

    static String getSubByKey(String key) {
        return FirebaseRemoteConfig.getInstance().getString(key);
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
        defaults.put(SUB_OFFER_WEEKLY, DEFAULT_WEEKLY);
        defaults.put(SUB_OFFER_TRIAL, DEFAULT_TRIAL);
        defaults.put(SUB_OFFER_LIFETIME, DEFAULT_LIFETIME);

        config.setDefaultsAsync(mergeDefaults(config, defaults)).addOnCompleteListener(t ->
                config.fetchAndActivate().addOnCompleteListener(task -> {

            if (t.isSuccessful())
                Log.d(BillingManager.LOG_TAG, "Defaults are set!");
            else
                Log.d(BillingManager.LOG_TAG, "Defaults are not set!: " + task.getException().toString());

            if (task.isSuccessful()) {
                Log.d(BillingManager.LOG_TAG, "Fetched new config!");
            } else {
                Log.d(BillingManager.LOG_TAG, "Fetch failed!");
            }

            String subTrial = config.getString(SUB_TRIAL);
            String subPremium = config.getString(SUB_PREMIUM);
            String offerWeekly = config.getString(SUB_OFFER_WEEKLY);
            String offerTrial = config.getString(SUB_OFFER_TRIAL);
            String offerLifetime = config.getString(SUB_OFFER_LIFETIME);

            Log.d(BillingManager.LOG_TAG, "Trial: " + subTrial);
            Log.d(BillingManager.LOG_TAG, "Premium: " + subPremium);
            Log.d(BillingManager.LOG_TAG, "Offer Weekly: " + offerWeekly);
            Log.d(BillingManager.LOG_TAG, "Offer Trial: " + offerTrial);
            Log.d(BillingManager.LOG_TAG, "Offer Lifetime: " + offerLifetime);

            listener.onComplete(task.isSuccessful());
        }));
    }

    private static Map<String, Object> mergeDefaults(FirebaseRemoteConfig config, Map<String, Object> newDefaults) {
        Map<String, FirebaseRemoteConfigValue> oldValues = config.getAll();
        Map<String, Object> oldDefaults = new HashMap<>();
        for (Map.Entry<String, FirebaseRemoteConfigValue> entry : oldValues.entrySet()) {
            if (entry.getValue().getSource() == FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT)
                oldDefaults.put(entry.getKey(), entry.getValue().asString());
        }
        oldDefaults.putAll(newDefaults);
        return oldDefaults;
    }
}
