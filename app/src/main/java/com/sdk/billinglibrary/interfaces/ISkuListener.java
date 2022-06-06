package com.sdk.billinglibrary.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.SkuDetails;

public interface ISkuListener {
    void onResult(@NonNull ProductDetails trial, @NonNull ProductDetails full);
    void onFailed();
}
