package com.sdk.billinglibrary.interfaces;

public interface ISubscriptionListener {
    void onResult(boolean isSubscribed);
    void onFailed(String error);
}
