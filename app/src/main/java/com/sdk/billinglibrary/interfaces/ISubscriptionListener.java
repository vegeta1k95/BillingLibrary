package com.sdk.billinglibrary.interfaces;

public interface ISubscriptionListener {
    void onResult(boolean isSubscribed);
     default void onFailed(String error) {}
}
