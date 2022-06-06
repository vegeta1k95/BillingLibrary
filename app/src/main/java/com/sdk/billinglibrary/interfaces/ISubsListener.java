package com.sdk.billinglibrary.interfaces;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.SkuDetails;

import java.util.List;

public interface ISubsListener {
    void onResult(boolean isSuccessful, List<ProductDetails> products);
}
