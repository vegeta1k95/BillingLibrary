package com.sdk.billinglibrary;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BillingFragment extends Fragment {

    @Keep
    public BillingFragment() { super(); }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Context context = getContext();
        String layoutName = getArguments().getString("layout");
        int layoutId = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
        return inflater.inflate(layoutId, container, false);
    }

    static BillingFragment getInstance(String layoutName) {
        BillingFragment fragment = new BillingFragment();
        Bundle params = new Bundle();
        params.putString("layout", layoutName);
        fragment.setArguments(params);
        return fragment;
    }

}
