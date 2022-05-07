package com.sdk.billinglibrary;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.android.billingclient.api.SkuDetails;

import java.util.Locale;

public class Price {

    private final Resources mResources;

    private final double mPriceCurrency;
    private final String mCurrencyCode;

    private final Period mSubscriptionPeriod;
    private final Period mTrialPeriod;

    public Price(Resources resources, @NonNull SkuDetails sku) {
        mResources = resources;
        mPriceCurrency = (double) sku.getPriceAmountMicros() / 1000000;
        mCurrencyCode = sku.getPriceCurrencyCode();

        mSubscriptionPeriod = Period.parse(sku.getSubscriptionPeriod());
        mTrialPeriod = Period.parse(sku.getFreeTrialPeriod());
    }

    public String getPriceAndCurrency() {
        return mCurrencyCode + String.format(Locale.getDefault(), " %.2f", mPriceCurrency);
    }

    public String getTrialPeriod() {
        if (mTrialPeriod.years > 0)
            return mResources.getQuantityString(R.plurals.years, mTrialPeriod.years, mTrialPeriod.years);
        else if (mTrialPeriod.months > 0)
            return mResources.getQuantityString(R.plurals.months, mTrialPeriod.months, mTrialPeriod.months);
        else if (mTrialPeriod.weeks > 0)
            return mResources.getQuantityString(R.plurals.weeks, mTrialPeriod.weeks, mTrialPeriod.weeks);
        else if (mTrialPeriod.days > 0)
            return mResources.getQuantityString(R.plurals.days, mTrialPeriod.days, mTrialPeriod.days);
        return "";
    }

    public String getSubscriptionPeriod() {
        if (mSubscriptionPeriod.years > 0)
            return mResources.getQuantityString(R.plurals.years, mSubscriptionPeriod.years, mSubscriptionPeriod.years);
        else if (mSubscriptionPeriod.months > 0)
            return mResources.getQuantityString(R.plurals.months, mSubscriptionPeriod.months, mSubscriptionPeriod.months);
        else if (mSubscriptionPeriod.weeks > 0)
            return mResources.getQuantityString(R.plurals.weeks, mSubscriptionPeriod.weeks, mSubscriptionPeriod.weeks);
        else if (mSubscriptionPeriod.days > 0)
            return mResources.getQuantityString(R.plurals.days, mSubscriptionPeriod.days, mSubscriptionPeriod.days);
        return "";
    }

    public String getTotalPriceAndCurrency() {
        if (mSubscriptionPeriod.years > 0)  // PER MONTH
            return mCurrencyCode + String.format(Locale.getDefault(), " %.2f", mPriceCurrency / (12 * mSubscriptionPeriod.years));
        else if (mSubscriptionPeriod.months > 0) // PER WEEK
            return mCurrencyCode + String.format(Locale.getDefault(), " %.2f", mPriceCurrency / (4.35 * mSubscriptionPeriod.months));
        else if (mSubscriptionPeriod.weeks > 0) // PER MONTH
            return mCurrencyCode + String.format(Locale.getDefault(), " %.2f", mPriceCurrency  * 4.35 / mSubscriptionPeriod.weeks);
        else if (mSubscriptionPeriod.days > 0) // PER WEEK
            return mCurrencyCode + String.format(Locale.getDefault(), " %.2f", mPriceCurrency * 7 / mSubscriptionPeriod.days);
        return "";
    }

    public String getTotalPeriod() {
        if (mSubscriptionPeriod.years > 0)
            return mResources.getString(R.string.month).toLowerCase();
        else if (mSubscriptionPeriod.months > 0)
            return mResources.getString(R.string.week).toLowerCase();
        else if (mSubscriptionPeriod.weeks > 0)
            return mResources.getString(R.string.month).toLowerCase();
        else if (mSubscriptionPeriod.days > 0)
            return mResources.getString(R.string.week).toLowerCase();
        return "";
    }
}
