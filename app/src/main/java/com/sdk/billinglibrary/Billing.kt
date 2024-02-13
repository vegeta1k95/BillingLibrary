package com.sdk.billinglibrary

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class BillingStatus {
    SUBSCRIBED,
    NOT_SUBSCRIBED,
    UNSUPPORTED
}

object Billing {

    const val LOG = "MYTAG (Billing)"
    const val UNSUPPORTED = "NOT_SUPPORTED"

    var onDismiss: (() -> Unit)? = null
    private var test = false

    lateinit var app: Application
    lateinit var manager: BillingManager

    fun initialize(
        application: Application,
        themeId: Int,
        testMode: Boolean) {

        app = application
        test = testMode

        manager = BillingManager()

        // Apply style to fetch attributes from XML
        val context = application.applicationContext
        context.theme.applyStyle(themeId, false)

        // Init shared preferences
        LocalConfig.init(context)

        // Request Firebase Remote Config
        RemoteConfig.fetchSubs(context) { isSuccessful: Boolean ->

            // Get fetched sub IDs
            val trialSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_TRIAL)
            val premiumSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_PREMIUM)

            // If one of fetched sub IDs is "NOT_SUPPORTED" we consider user "subscribed"
            if (trialSubId == UNSUPPORTED || premiumSubId == UNSUPPORTED) {

                // Denote billing as unsupported only if there is no currently active subscription
                // (to avoid currently subscribed users receive ads, etc)
                if (LocalConfig.getCurrentSubscription() == null) LocalConfig.subscribeLocally(
                    UNSUPPORTED
                )

                // Do not init billing in this case - unnecessary.
                return@fetchSubs
            }

            // Otherwise initialize billing
            manager.initialize()
        }
    }

    fun getStatus(): BillingStatus {

        val sub = LocalConfig.getCurrentSubscription()

        return when {
            test -> BillingStatus.SUBSCRIBED
            sub.isNullOrEmpty() -> BillingStatus.NOT_SUBSCRIBED
            sub == UNSUPPORTED -> BillingStatus.UNSUPPORTED
            else -> BillingStatus.SUBSCRIBED
        }
    }

    fun canShowAds(): Boolean {
        val status = getStatus()
        return status == BillingStatus.NOT_SUBSCRIBED || status == BillingStatus.UNSUPPORTED
    }

    fun canShowBilling(): Boolean {
        val status = getStatus()
        return status == BillingStatus.NOT_SUBSCRIBED
    }

    fun manageSubs(activity: Activity) {
        var url = "https://play.google.com/store/account/subscriptions"
        val sub = LocalConfig.getCurrentSubscription()
        if (sub != null && !test && sub != UNSUPPORTED) {
            url += "?sku=" + sub + "&package=" + activity.packageName
        }
        val page = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, page)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    fun startBillingActivity(context: Context?) {
        startBillingActivity(context, null)
    }

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
