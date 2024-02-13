package com.sdk.billinglibrary

import android.content.Context
import android.util.Log
import android.util.TypedValue
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig

object RemoteConfig {

    const val KEY_TRIAL = "sub_trial"
    const val KEY_PREMIUM = "sub_premium"

    fun getSubByKey(key: String?): String {
        return FirebaseRemoteConfig.getInstance().getString(key!!)
    }

    fun fetchSubs(context: Context, listener: (Boolean) -> Unit) {

        val config = Firebase.remoteConfig
        val defaults = constructDefaults(context)
        
        config.setDefaultsAsync(defaults)
            .addOnCompleteListener { t: Task<Void?> ->
                config.fetchAndActivate().addOnCompleteListener { task: Task<Boolean?> ->

                    if (t.isSuccessful)
                        Log.d(Billing.LOG, "Defaults are set!")
                    else
                        Log.d(Billing.LOG, "Defaults are not set!: ${task.exception.toString()}")

                    if (task.isSuccessful)
                        Log.d(Billing.LOG, "Fetched new config!")
                    else
                        Log.d(Billing.LOG, "Fetch failed!")

                    val subTrial = config.getString(KEY_TRIAL)
                    val subPremium = config.getString(KEY_PREMIUM)

                    Log.d(Billing.LOG, "Trial: $subTrial")
                    Log.d(Billing.LOG, "Premium: $subPremium")

                    listener.invoke(task.isSuccessful)
                }
            }
    }

    private fun constructDefaults(context: Context): MutableMap<String, Any> {

        val defaults: MutableMap<String, Any> = HashMap()

        // Fill defaults with already existing defaults (if they were initialized before)
        val oldValues = Firebase.remoteConfig.all
        for ((key, value) in oldValues)
        {
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT)
                defaults[key] = value.asString()
        }

        // Add billing defaults
        val trial = TypedValue()
        val premium = TypedValue()

        context.theme.resolveAttribute(R.attr.billing_default_premium, premium, true)
        context.theme.resolveAttribute(R.attr.billing_default_trial, trial, true)

        defaults[KEY_TRIAL] = trial.coerceToString()
        defaults[KEY_PREMIUM] = premium.coerceToString()
        
        return defaults
    }
    
}
