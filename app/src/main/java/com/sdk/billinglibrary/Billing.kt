package com.sdk.billinglibrary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.core.net.toUri
import com.google.firebase.analytics.FirebaseAnalytics

interface BillingPurchaseListener {
    fun onPurchaseDone() {}
    fun onPurchaseFail() {}
    fun onPurchaseCancelled() {}
    fun onError() {}
}

enum class BillingStatus {
    SUBSCRIBED, NOT_SUBSCRIBED, UNSUPPORTED
}

enum class BillingMode {
    LOCKED, UNLOCKED, TEST
}

@SuppressLint("StaticFieldLeak")
object Billing {

    const val LOG = "MYTAG (Billing)"
    const val UNSUPPORTED = "NOT_SUPPORTED"

    private var mode = BillingMode.LOCKED

    internal val listeners = mutableListOf<BillingPurchaseListener>()
    internal lateinit var manager: BillingManager
    internal var firebaseAnalyticsID: String? = null

    val isInitialized = MutableLiveData(false)
    val products = HashMap<String, Price>()

    @JvmStatic
    fun initialize(
        application: Application,
        billingMode: BillingMode = BillingMode.LOCKED,
        vararg subIds: String) {

        Log.d(LOG, "Initialization of billing...")

        FirebaseAnalytics.getInstance(application).appInstanceId.addOnSuccessListener {
            Log.d(LOG, "FirebaseAnalytics ID: $it")
            firebaseAnalyticsID = it
        }

        mode = billingMode
        manager = BillingManager(application.applicationContext)

        // Apply style to fetch attributes from XML
        val context = application.applicationContext

        // Init shared preferences
        LocalConfig.init(context)

        // If test mode ON - do not initialize billing manager / remote config (all data will
        // be fictional)
        if (mode == BillingMode.TEST) {
            subIds.forEach {
                if (it.contains("trial"))
                    products[it] = Price.createTestPrice(context, trial = true)
                else
                    products[it] = Price.createTestPrice(context, trial = false)
            }
            isInitialized.postValue(true)
            return
        }

        manager.initialize(*subIds)
    }

    @JvmStatic
    fun addListener(listener: BillingPurchaseListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: BillingPurchaseListener) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun launchFlow(activity: Activity?, product: Price) {
        activity
        if (mode == BillingMode.TEST)
            Toast.makeText(activity, "(TEST) Purchase flow launched!", Toast.LENGTH_LONG).show()
        else
            manager.launchPurchaseFlow(activity, product)
    }

    @JvmStatic
    fun getStatus(): BillingStatus {

        val sub = LocalConfig.getCurrentSubscription()

        return when {
            mode == BillingMode.UNLOCKED -> BillingStatus.SUBSCRIBED
            sub == UNSUPPORTED           -> BillingStatus.UNSUPPORTED
            sub.isNullOrEmpty()          -> BillingStatus.NOT_SUBSCRIBED
            else                         -> BillingStatus.SUBSCRIBED
        }
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
    fun manageSubs(context: Context?) {
        var url = "https://play.google.com/store/account/subscriptions"
        val sub = LocalConfig.getCurrentSubscription()
        if (sub != null && sub != UNSUPPORTED) {
            url += "?sku=$sub&package=${context?.packageName}"
        }
        val page = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, page)
        try {
            context?.startActivity(intent)
        } catch ( _ : Exception) {
            // Ignore
        }
    }
}
