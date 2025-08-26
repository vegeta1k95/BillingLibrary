package com.sdk.billinglibrary

import android.content.res.Resources
import com.android.billingclient.api.ProductDetails
import java.util.Locale

fun Double.format(digits: Int) = "%.${digits}f".format(this)

class Price(
    val product: ProductDetails?) {

    companion object {

        fun createTestPrice(trial: Boolean) : Price {
            val price = Price(null)

            price.price = 12.34
            price.currency = "UAH"

            if (trial) {
                price.periodTrial = Period.parse("P3D")
                price.periodPremium = Period.parse("P1W")
            } else {
                price.periodPremium = Period.parse("P1Y")
            }

            return price
        }
    }

    private val offer = product?.subscriptionOfferDetails?.get(0)
    private val res: Resources = Billing.app.resources

    var price: Double = 0.0
    var currency: String = "USD"

    var periodTrial: Period? = null
    var periodPremium: Period? = null

    val token: String? = offer?.offerToken

    init {

        val phases = offer?.pricingPhases?.pricingPhaseList

        if (phases != null) {

            for (phase in phases)
            {
                if (phase.priceAmountMicros == 0L)
                {
                    periodTrial = Period.parse(phase.billingPeriod)
                }
                else
                {
                    price = phase.priceAmountMicros.toDouble() / 1000000
                    currency = phase.priceCurrencyCode
                    periodPremium = Period.parse(phase.billingPeriod)
                }
            }
        }

    }

    fun isTrial() : Boolean {
        return periodTrial != null
    }

    fun getPriceAndCurrency(): String {
        return "$currency ${price.format(2)}"
    }

    fun getTrialPeriod(): String {
        val period = periodTrial ?: return res.getQuantityString(R.plurals.days, 0, 0)
        if (period.years > 0)
            return if (period.years == 1)
                res.getString(R.string.year)
            else
                res.getQuantityString(R.plurals.years, period.years, period.years)
        else if (period.months > 0)
            return if (period.months == 1)
                res.getString(R.string.month)
            else
                res.getQuantityString(R.plurals.months, period.months, period.months)
        else if (period.weeks > 0)
            return if (period.weeks == 1)
                res.getString(R.string.week)
            else
                res.getQuantityString(R.plurals.weeks, period.weeks, period.weeks)
        else if (period.days > 0)
            return if (period.days == 1)
                res.getString(R.string.day)
            else
                res.getQuantityString(R.plurals.days, period.days, period.days)
        return ""
    }

    fun getSubscriptionPeriod(): String {
        val period = periodPremium ?: return ""
        if (period.years > 0)
            return if (period.years == 1)
                res.getString(R.string.year)
            else
                res.getQuantityString(R.plurals.years, period.years, period.years)
        else if (period.months > 0)
            return if (period.months == 1)
                res.getString(R.string.month)
            else
                res.getQuantityString(R.plurals.months, period.months, period.months)
        else if (period.weeks > 0)
            return if (period.weeks == 1)
                res.getString(R.string.week)
            else
                res.getQuantityString(R.plurals.weeks, period.weeks, period.weeks)
        else if (period.days > 0)
            return if (period.days == 1)
                res.getString(R.string.day)
            else
                res.getQuantityString(R.plurals.days, period.days, period.days)
        return ""
    }

    fun getTotalPriceAndCurrency(): String {
        val period = periodPremium ?: return ""
        var amount = 0.0
        if (period.years > 0)
            amount = price / (12 * period.years)        // PER MONTH
        else if (period.months > 0)
            amount = price / (4.35 * period.months)     // PER WEEK
        else if (period.weeks > 0)
            amount = price * 4.35 / period.weeks        // PER MONTH
        else if (period.days > 0)
            amount = price * 7 / period.days            // PER WEEK

        return if (amount > 0)
            "$currency ${amount.format(2)}"
        else
            ""
    }

    fun getTotalPeriod(): String {
        val period = periodPremium ?: return ""
        if (period.years > 0) 
            return res.getString(R.string.month).lowercase(Locale.getDefault()) 
        else if (period.months > 0) 
            return res.getString(R.string.week).lowercase(Locale.getDefault()) 
        else if (period.weeks > 0) 
            return res.getString(R.string.month).lowercase(Locale.getDefault()) 
        else if (period.days > 0) 
            return res.getString(R.string.week).lowercase(Locale.getDefault())
        return ""
    }

}