package com.sdk.billinglibrary

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.core.net.toUri

enum class BillingStatus {
    SUBSCRIBED,
    NOT_SUBSCRIBED,
    UNSUPPORTED
}

object Billing {

    const val LOG = "MYTAG (Billing)"
    const val UNSUPPORTED = "NOT_SUPPORTED"

    var onDismiss: (() -> Unit)? = null
    private var unlocked = false
    var test = false

    lateinit var app: Application
    lateinit var manager: BillingManager

    val subChosen = MutableLiveData<Price>()

    @JvmStatic
    fun setActivity(activity: Activity?) {
        manager.activity = activity
    }

    @JvmStatic
    fun launchFlow(activity: Activity?) {
        manager.launchPurchaseFlow(activity)
    }

    @JvmStatic
    fun toggleSub() {
        if (subChosen.value == manager.subTrial) {
            subChosen.postValue(manager.subFull)
        } else if (subChosen.value == manager.subFull) {
            subChosen.postValue(manager.subTrial)
        }
    }

    @JvmStatic
    fun initialize(
        application: Application,
        themeId: Int,
        unlockedMode: Boolean,
        testMode: Boolean) {

        Log.d(LOG, "Initialization of billing...")

        app = application
        unlocked = unlockedMode
        test = testMode

        manager = BillingManager()

        // Apply style to fetch attributes from XML
        val context = application.applicationContext
        context.theme.applyStyle(themeId, false)

        // Init shared preferences
        LocalConfig.init(context)

        // If test mode ON - do not initialize billing manager / remote config (all data will
        // be fictional)
        if (test) {
            manager.subTrial = Price.createTestPrice(trial = true)
            manager.subFull = Price.createTestPrice(trial = false)
            subChosen.postValue(manager.subTrial)
            manager.initialized.complete(Unit)
            return
        }

        // Request Firebase Remote Config
        RemoteConfig.fetchSubs(context) { isSuccessful: Boolean ->

            // Get fetched sub IDs
            val trialSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_TRIAL)
            val premiumSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_PREMIUM)

            // If one of fetched sub IDs is "NOT_SUPPORTED" we consider user "subscribed"
            if (trialSubId == UNSUPPORTED || premiumSubId == UNSUPPORTED) {

                // Denote billing as unsupported only if there is no currently active subscription
                // (to avoid currently subscribed users receive ads, etc)
                if (LocalConfig.getCurrentSubscription() == null)
                    LocalConfig.subscribeLocally(UNSUPPORTED)

                // Do not init billing in this case - unnecessary.
                return@fetchSubs
            }

            // Otherwise initialize billing
            manager.initialize()
        }
    }

    @JvmStatic
    fun getStatus(): BillingStatus {

        val sub = LocalConfig.getCurrentSubscription()

        return when {
            unlocked -> BillingStatus.SUBSCRIBED
            sub.isNullOrEmpty() -> BillingStatus.NOT_SUBSCRIBED
            sub == UNSUPPORTED -> BillingStatus.UNSUPPORTED
            else -> BillingStatus.SUBSCRIBED
        }
    }

    @JvmStatic
    fun getVersion(): String {
        return RemoteConfig.getVersion()
    }

    @JvmStatic
    fun canShowAds(): Boolean {
        val status = getStatus()
        return status == BillingStatus.NOT_SUBSCRIBED || status == BillingStatus.UNSUPPORTED
    }

    @JvmStatic
    fun canShowBilling(): Boolean {
        val status = getStatus()
        return status == BillingStatus.NOT_SUBSCRIBED
    }

    @JvmStatic
    fun manageSubs(activity: Activity) {
        var url = "https://play.google.com/store/account/subscriptions"
        val sub = LocalConfig.getCurrentSubscription()
        if (sub != null && !unlocked && sub != UNSUPPORTED) {
            url += "?sku=" + sub + "&package=" + activity.packageName
        }
        val page = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, page)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    @JvmStatic
    fun startBillingActivity(context: Context?) {
        startBillingActivity(context, null)
    }

    @JvmStatic
    fun startBillingActivity(context: Context?, onDismiss: (() -> Unit)? = null) {
        if (context == null)
        {
            onDismiss?.invoke()
            return
        }
        this.onDismiss = onDismiss
        context.startActivity(Intent(context, BillingActivity::class.java))
    }
}
