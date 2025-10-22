package com.sdk.billinglibrary

import com.android.billingclient.api.ProductDetails

fun Double.format(digits: Int) = "%.${digits}f".format(this)

class Price(
    val product: ProductDetails?) {
    val productId: String? = product?.productId
    private val offer = product?.subscriptionOfferDetails?.getOrNull(0)

    var paymentAmount: Double = 0.0
    var paymentCurrency: String = "USD"
    var paymentInterval: Period? = null

    var periodTrial: Period? = null

    val token: String? = offer?.offerToken

    init {
        offer?.pricingPhases?.pricingPhaseList?.forEach { phase ->
            if (phase.priceAmountMicros == 0L) {
                periodTrial = Period.parse(phase.billingPeriod)
            } else {
                paymentAmount = phase.priceAmountMicros.toDouble() / 1000000
                paymentCurrency = phase.priceCurrencyCode
                paymentInterval = Period.parse(phase.billingPeriod)
            }
        }
    }

    fun pricePer(target: Interval): Double {
        val period = paymentInterval ?: return 0.0
        val baseDays = period.totalDays()
        if (baseDays <= 0.0) return 0.0

        val perDay = paymentAmount / baseDays
        val targetDays = when (target) {
            Interval.DAY -> 1.0
            Interval.WEEK -> Period.DAYS_PER_WEEK
            Interval.MONTH -> Period.DAYS_PER_MONTH
            Interval.YEAR -> Period.DAYS_PER_YEAR
        }
        return perDay * targetDays
    }

    fun isTrial() : Boolean {
        return periodTrial != null
    }

    companion object {

        fun createTestPrice(trial: Boolean) : Price {
            val price = Price(null)

            price.paymentAmount = 12.34
            price.paymentCurrency = "UAH"

            if (trial) {
                price.periodTrial = Period.parse("P3D")
                price.paymentInterval = Period.parse("P1W")
            } else {
                price.paymentInterval = Period.parse("P1Y")
            }

            return price
        }
    }

}