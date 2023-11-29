package com.sdk.billinglibrary;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

class LocalConfig {

    private static final String PREFERENCES = "billing";
    private static final String KEY_SUBSCRIBED = "subscribed";
    private static final String KEY_IS_FIRST_TIME_BILLING = "first_time";

    private static SharedPreferences preferences;

    static void init(Context context) {
        if (preferences == null)
            preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static void subscribeLocally(String subId) {
        if (subId != null)
            preferences.edit().putString(KEY_SUBSCRIBED, subId).apply();
        else
            preferences.edit().remove(KEY_SUBSCRIBED).apply();
    }

    static @Nullable String getCurrentSubscription() {
        return preferences.getString(KEY_SUBSCRIBED, null);
    }

    static boolean isFirstTimeBilling() {
        return !preferences.contains(KEY_IS_FIRST_TIME_BILLING);
    }

    static void didFirstBilling() {
        preferences.edit().putBoolean(KEY_IS_FIRST_TIME_BILLING, true).apply();
    }
}
