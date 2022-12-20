package com.sdk.billinglibrary;

import android.content.Context;
import android.content.SharedPreferences;

class LocalConfig {

    private static final String PREFERENCES = "billing";
    private static final String KEY_SUBSCRIBED = "subscribed";
    private static final String KEY_LAST_PROPOSED = "last_proposed";
    private static final String KEY_IS_FIRST_TIME_BILLING = "first_time";
    private static final String KEY_IS_FIRST_TIME_OFFER = "first_time_offer";

    private static SharedPreferences preferences;

    static void init(Context context) {
        if (preferences == null)
            preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static void subscribeLocally(boolean subscribed) {
        preferences.edit().putBoolean(KEY_SUBSCRIBED, subscribed).apply();
    }

    static boolean isSubscribedLocally() {
        return preferences.getBoolean(KEY_SUBSCRIBED, false);
    }

    static boolean isFirstTimeBilling() {
        return !preferences.contains(KEY_IS_FIRST_TIME_BILLING);
    }

    static void didFirstBilling() {
        preferences.edit().putBoolean(KEY_IS_FIRST_TIME_BILLING, true).apply();
    }

    static boolean isFirstTimeOffer() {
        long firstLaunch = preferences.getLong(KEY_IS_FIRST_TIME_OFFER, 0L);
        if (firstLaunch == 0) {
            preferences.edit().putLong(KEY_IS_FIRST_TIME_OFFER, System.currentTimeMillis()).apply();
            return true;
        } else
            return false;
    }

    static boolean daysPassedSinceFirstOffer(int days) {
        long firstLaunch = preferences.getLong(KEY_IS_FIRST_TIME_OFFER, 0L);
        if (firstLaunch == 0)
            return false;
        return System.currentTimeMillis() - firstLaunch >= days * 8600000L;
    }
}
