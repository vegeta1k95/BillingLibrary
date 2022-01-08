package com.sdk.billinglibrary.onboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sdk.billinglibrary.BillingActivity;
import com.sdk.billinglibrary.LocalConfig;
import com.sdk.billinglibrary.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class OnBoardActivity extends AppCompatActivity {

    public static final String INTENT_ONLY_ONBOARD = "only_onboard";

    public static List<String> onBoardLayouts = new ArrayList<>();
    static class PageAdapter extends FragmentStateAdapter {

        PageAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return OnBoardFragment.getInstance(onBoardLayouts.get(position));
        }

        @Override
        public int getItemCount() {
            return onBoardLayouts.size();
        }
    }

    private PageAdapter adapter;
    private ViewPager2 pager;

    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_board);

        boolean onlyOnBoard = getIntent().getBooleanExtra(INTENT_ONLY_ONBOARD, false);

        Window window = getWindow();
        window.setStatusBarColor(MaterialColors.getColor(this, R.attr.billing_status_bar, R.attr.colorAccent));

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(0x55000000);


        adapter = new PageAdapter(this);
        pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);

        TabLayout tabs = findViewById(R.id.tabDots);
        tabs.setTabRippleColor(null);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {}).attach();

        Button btnContinue = findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current == adapter.getItemCount() - 1) {
                LocalConfig.setOnBoardShown();
                LocalConfig.setConsent(true);
                if (!onlyOnBoard) {
                    Intent intent = new Intent(OnBoardActivity.this, BillingActivity.class);
                    startActivity(intent);
                }
                finish();
            } else {
                pager.setCurrentItem(current+1);
            }
        });
    }

    public static void setWindowFlag(Activity activity, final int bits, boolean on) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }
}
