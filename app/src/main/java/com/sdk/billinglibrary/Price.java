package com.sdk.billinglibrary;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;

import java.util.Locale;

public class Price {

    private final ProductDetails.SubscriptionOfferDetails mOffer;
    private final Resources mResources;

    private final String mToken;

    private double mPriceCurrency;
    private String mCurrencyCode;

    private Period mSubscriptionPeriod;
    private Period mTrialPeriod;

    public Price(Resources resources, @NonNull ProductDetails.SubscriptionOfferDetails offer) {
        mResources = resources;
        mOffer = offer;
        mToken = offer.getOfferToken();
        parseOffer();
    }

    private void parseOffer() {

        for (ProductDetails.PricingPhase phase : mOffer.getPricingPhases().getPricingPhaseList()) {
            if (phase.getPriceAmountMicros() == 0) {
                mTrialPeriod = Period.parse(phase.getBillingPeriod());
            } else {
                mPriceCurrency = (double) phase.getPriceAmountMicros() / 1000000;
                mCurrencyCode = phase.getPriceCurrencyCode();
                mSubscriptionPeriod = Period.parse(phase.getBillingPeriod());
            }

        }
    }

    public String getToken() {
        return mToken;
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
