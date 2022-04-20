package com.sdk.billinglibrary.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.SkuDetails;

public interface ISkuListener {
    void onResult(@NonNull SkuDetails trial, @NonNull SkuDetails full);
    void onFailed();
}
