package com.sdk.billinglibrary;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalConfig {

    private static final String PREFERENCES = "ONBOARD";
    private static final String KEY_ONBOARD_PASSED = "passed";
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
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_LAST_PROPOSED, System.currentTimeMillis());
        editor.commit();
    }

    static boolean isOnBoardShown() {
        return preferences.getBoolean(KEY_ONBOARD_PASSED, false);
    }

    static void subscribeLocally(boolean subscribed) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_SUBSCRIBED, subscribed);
        editor.commit();
    }

    public static boolean isSubscribedLocally() {
        return preferences.getBoolean(KEY_SUBSCRIBED, false);
    }

    public static void setOnBoardShown() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_ONBOARD_PASSED, true);
        editor.commit();
    }

    public static void setConsent(boolean consent) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_CONSENT, consent);
        editor.commit();
    }

    public static boolean getConsent() {
        return preferences.getBoolean(KEY_CONSENT, true);
    }

}
