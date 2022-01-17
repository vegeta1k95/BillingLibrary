package com.sdk.billinglibrary;

import android.content.Context;
import android.content.SharedPreferences;

class LocalConfig {

    private static final String PREFERENCES = "billing";
    private static final String KEY_SUBSCRIBED = "subscribed";
    private static final String KEY_LAST_PROPOSED = "last_proposed";
    private static final String KEY_CONSENT = "consent";

    private static SharedPreferences preferences;

    static void init(Context context) {
        if (preferences == null)
            preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    static boolean isTimeToPropose() {
        long lastProposed = preferences.getLong(KEY_LAST_PROPOSED, 0);
        return lastProposed == 0 || System.currentTimeMillis() - lastProposed >= 60000;
    }

    static void setTimeProposed() {
        preferences.edit().putLong(KEY_LAST_PROPOSED, System.currentTimeMillis()).apply();
    }

    static void subscribeLocally(boolean subscribed) {
        preferences.edit().putBoolean(KEY_SUBSCRIBED, subscribed).apply();
    }

    static boolean isSubscribedLocally() {
        return preferences.getBoolean(KEY_SUBSCRIBED, false);
    }

}
