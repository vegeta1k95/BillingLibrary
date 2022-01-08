package com.sdk.billinglibrary.interfaces;

import com.android.billingclient.api.SkuDetails;

public interface ISkuListener {
    void onResult(SkuDetails trial, SkuDetails full);
    void onFailed();
}
