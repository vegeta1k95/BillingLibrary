package com.sdk.billinglibrary

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.sdk.billinglibrary.LocalConfig.subscribeLocally

private val BANNED_CURRENCIES = listOf("INR", "MYR")

class BillingManager(
    val context: Context) {

    private val onPurchase: BillingPurchaseListener = object : BillingPurchaseListener {
        override fun onPurchaseDone() {
            Toast.makeText(context, R.string.purchase_done, Toast.LENGTH_LONG).show()
            Billing.listeners.forEach { it.onPurchaseDone() }
        }

        override fun onPurchaseFail() {
            Toast.makeText(context, R.string.purchase_fail, Toast.LENGTH_LONG).show()
            Billing.listeners.forEach { it.onPurchaseFail() }
        }

        override fun onPurchaseCancelled() {
            Billing.listeners.forEach { it.onPurchaseCancelled() }
        }

        override fun onError() {
            Toast.makeText(context, R.string.purchase_fail, Toast.LENGTH_LONG).show()
            Billing.listeners.forEach { it.onError() }
        }
    }

    private val client: BillingClient = BillingClient
        .newBuilder(context)
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
        .enablePendingPurchases(PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts().build())
        .build()

    fun initialize(vararg productIds: String) {
        Log.d(Billing.LOG, "Initialization of billing manager...")
        client.startConnection(object : BillingClientStateListener {

            override fun onBillingServiceDisconnected() {
                // client.startConnection(this)
            }

            override fun onBillingSetupFinished(result: BillingResult) {

                if (Billing.isInitialized.value == true)
                    return

                if (result.responseCode == BillingClient.BillingResponseCode.OK)
                {
                    Log.d(Billing.LOG, "Billing setup finished!")
                    if (isSubscriptionSupported())
                    {
                        Log.d(Billing.LOG, "Subscriptions are supported!")
                        querySubsPurchases()
                        querySubsProducts(*productIds)
                    }
                    else
                    {
                        Log.d(Billing.LOG, "Subscription are not supported!")
                        subscribeLocally(Billing.UNSUPPORTED)
                        Billing.isInitialized.postValue(true)
                    }
                }
                else
                {
                    Log.d(Billing.LOG, "Billing setup failed: ${result.responseCode} | ${result.debugMessage}")
                    Billing.isInitialized.postValue(true)
                }
            }
        })
    }

    private fun isSubscriptionSupported(): Boolean {
        val code = client.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode
        return code == BillingClient.BillingResponseCode.OK
    }

    private fun querySubsProducts(vararg ids: String) {

        val builder = QueryProductDetailsParams.Product.newBuilder()
        val products: MutableList<QueryProductDetailsParams.Product> = ArrayList()

        // Create a list of Products
        ids.forEach {
            products.add(
                builder
                    .setProductId(it)
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
                    }

                    Billing.products[sub.productId] = Price(context,sub)
                }
            }
            else
            {
                Log.d(Billing.LOG, "Failed to fetch ${list.size} SUBS!")
            }

            Billing.isInitialized.postValue(true)
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

    fun launchPurchaseFlow(activity: Activity?, sub: Price) {

        if (!client.isReady || activity == null) {
            onPurchase.onError()
            return
        }

        try
        {
            val params: MutableList<ProductDetailsParams> = ArrayList()
            params.add(
                ProductDetailsParams.newBuilder()
                    .setProductDetails(sub.product!!)
                    .setOfferToken(sub.token!!)
                    .build()
            )


            var flowParamsBuilder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(params)

            Billing.firebaseAnalyticsID?.let { it ->
                flowParamsBuilder = flowParamsBuilder.setObfuscatedAccountId(it)
            }

            client.launchBillingFlow(activity, flowParamsBuilder.build())
        }
        catch ( _ : IllegalArgumentException)
        {
            onPurchase.onError()
        }
    }
}