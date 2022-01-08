package com.sdk.billinglibrary.interfaces;

public interface IOnPurchaseListener {
    void onPurchaseDone();
    void onPurchaseFail();
    void onPurchaseCancelled();
    void onError();
}