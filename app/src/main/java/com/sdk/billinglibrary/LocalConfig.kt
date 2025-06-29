package com.sdk.billinglibrary

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object LocalConfig {

    private const val PREFERENCES = "billing"
    private const val KEY_SUBSCRIBED = "subscribed"
    private const val KEY_IS_FIRST_TIME_BILLING = "first_time"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }

    fun subscribeLocally(subId: String?) {
        if (subId != null)
            preferences.edit { putString(KEY_SUBSCRIBED, subId) }
        else
            preferences.edit { remove(KEY_SUBSCRIBED) }
    }

    fun getCurrentSubscription(): String? {
        return preferences.getString(KEY_SUBSCRIBED, null)
    }

    fun isFirstTimeBilling(): Boolean {
        return !preferences.contains(KEY_IS_FIRST_TIME_BILLING)
    }

    fun didFirstBilling() {
        preferences.edit { putBoolean(KEY_IS_FIRST_TIME_BILLING, true) }
    }
}
