package com.sdk.billinglibrary

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.sdk.billinglibrary.LocalConfig.subscribeLocally
import kotlinx.coroutines.CompletableDeferred

private val BANNED_CURRENCIES = listOf("INR", "MYR")

class BillingManager {

    private var onPurchase: IOnPurchaseListener = object : IOnPurchaseListener {
        override fun onPurchaseDone() {
            Toast.makeText(Billing.app, R.string.purchase_done, Toast.LENGTH_LONG).show()
            Billing.onDismiss?.invoke()
        }

        override fun onPurchaseFail() {
            Toast.makeText(Billing.app, R.string.purchase_fail, Toast.LENGTH_LONG).show()
            Billing.onDismiss?.invoke()
        }

        override fun onPurchaseCancelled() {}
        override fun onError() {
            Toast.makeText(Billing.app, R.string.purchase_fail, Toast.LENGTH_LONG).show()
            Billing.onDismiss?.invoke()
        }
    }

    private val client: BillingClient = BillingClient
        .newBuilder(Billing.app)
        .setListener { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                val purchased = acknowledgePurchases(list)
                if (purchased)
                    onPurchase.onPurchaseDone()
                else
                    onPurchase.onPurchaseCancelled()
            } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                onPurchase.onPurchaseCancelled()
            } else {
                onPurchase.onPurchaseFail()
            }
        }
        .enablePendingPurchases()
        .build()

    var subTrial: Price? = null
    var subFull: Price? = null

    val initialized = CompletableDeferred<Unit>()

    fun initialize() {
        Log.d(Billing.LOG, "Initialization of billing manager...")
        client.startConnection(object : BillingClientStateListener {

            override fun onBillingServiceDisconnected() {
                // client.startConnection(this)
            }

            override fun onBillingSetupFinished(result: BillingResult) {

                if (initialized.isCompleted)
                    return

                if (result.responseCode == BillingClient.BillingResponseCode.OK)
                {
                    Log.d(Billing.LOG, "Billing setup finished!")
                    if (isSubscriptionSupported())
                    {
                        Log.d(Billing.LOG, "Subscriptions are supported!")
                        querySubsPurchases()
                        querySubsProducts()
                    }
                    else
                    {
                        Log.d(Billing.LOG, "Subscription are not supported!")
                        subscribeLocally(Billing.UNSUPPORTED)
                        initialized.complete(Unit)
                    }
                }
                else
                {
                    Log.d(Billing.LOG, "Billing setup failed: ${result.responseCode} | ${result.debugMessage}")
                    initialized.complete(Unit)
                }
            }
        })
    }

    private fun isSubscriptionSupported(): Boolean {
        val code = client.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode
        return code == BillingClient.BillingResponseCode.OK
    }

    private fun querySubsProducts() {

        // Get list of Sub IDs
        val trialSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_TRIAL)
        val premiumSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_PREMIUM)
        val subIds: MutableList<String> = ArrayList()
        subIds.add(trialSubId)
        subIds.add(premiumSubId)

        // Create a list of Products
        val products: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        val builder = QueryProductDetailsParams.Product.newBuilder()

        for (subId in subIds)
        {
            products.add(
                builder
                    .setProductId(subId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }

        // Create query
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        // Async request
        client.queryProductDetailsAsync(params) {
                result: BillingResult,
                list: List<ProductDetails?> ->

            if (result.responseCode == BillingClient.BillingResponseCode.OK)
            {
                Log.d(Billing.LOG, "Fetched ${list.size} SUBS")

                list.filterNotNull().forEach { sub ->

                    // If shitty country/currency - mark as billing unsupported.
                    if (sub.subscriptionOfferDetails?.any { subOffer ->
                            subOffer.pricingPhases.pricingPhaseList.any {
                               BANNED_CURRENCIES.contains(it.priceCurrencyCode.uppercase())
                           }
                        } == true)
                    {
                        subscribeLocally(Billing.UNSUPPORTED)
                        //Log.d(Billing.LOG, "Found unsupported currency. Disable billing.")
                    }
                }

                val trialSku = list.firstOrNull { it?.productId == trialSubId }
                val fullSku = list.firstOrNull { it?.productId == premiumSubId }

                if (trialSku != null) {
                    subTrial = Price(trialSku)
                    Billing.subChosen.postValue(subTrial)
                }

                if (fullSku != null) {
                    subFull = Price(fullSku)
                    Billing.subChosen.postValue(subFull)
                }

            }
            else
            {
                Log.d(Billing.LOG, "Failed to fetch ${list.size} SUBS!")
            }

            initialized.complete(Unit)
        }
    }

    private fun querySubsPurchases() {

        // Create query
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        // Async request
        client.queryPurchasesAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK)
            {
                acknowledgePurchases(list)
                Log.d(Billing.LOG, "Purchases are acknowledged!")
            }
            else
            {
                Log.d(Billing.LOG, "Failed to query purchases: ${result.responseCode} | ${result.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchases(list: List<Purchase>): Boolean {
        var purchasedSub: String? = null
        list.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
            {
                if (!purchase.isAcknowledged)
                {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    client.acknowledgePurchase(params) { }
                }
                purchasedSub = purchase.products[0]
            }
        }
        subscribeLocally(purchasedSub)
        return purchasedSub != null
    }

    fun launchPurchaseFlow(activity: Activity?, specificProduct: Price? = null) {

        val sub = specificProduct ?: Billing.subChosen.value

        if (!client.isReady || sub == null) {
            onPurchase.onError()
            return
        }

        try
        {

            val params: MutableList<ProductDetailsParams> = ArrayList()
            params.add(
                ProductDetailsParams.newBuilder()
                    .setProductDetails(sub.product)
                    .setOfferToken(sub.token)
                    .build()
            )
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(params).build()
            client.launchBillingFlow(activity!!, flowParams)
        }
        catch (ignore: IllegalArgumentException)
        {
            onPurchase.onError()
        }
    }
}