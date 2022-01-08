package com.sdk.billinglibrary;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.billingclient.api.SkuDetails;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;
import com.sdk.billinglibrary.interfaces.ISkuListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BillingActivity extends AppCompatActivity {

    static List<String> billingLayouts = new ArrayList<>();
    static class PageAdapter extends FragmentStateAdapter {

        PageAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return BillingFragment.getInstance(billingLayouts.get(position));
        }

        @Override
        public int getItemCount() {
            return billingLayouts.size();
        }
    }

    private IOnPurchaseListener onPurchaseListener = new IOnPurchaseListener() {
        @Override
        public void onPurchaseDone() {
            Toast.makeText(getApplicationContext(), R.string.purchase_done, Toast.LENGTH_LONG).show();
            finish();
            overridePendingTransition(R.anim.from_right, R.anim.to_left);
        }

        @Override
        public void onPurchaseFail() {
            Toast.makeText(getApplicationContext(), R.string.purchase_fail, Toast.LENGTH_LONG).show();
            finish();
            overridePendingTransition(R.anim.from_right, R.anim.to_left);
        }

        @Override
        public void onPurchaseCancelled() {}

        @Override
        public void onError() {
            Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
            finish();
            overridePendingTransition(R.anim.from_right, R.anim.to_left);
        }
    };

    private int counter = 3600*24;

    private Handler handler;

    private PageAdapter adapter;
    private ViewPager2 pager;

    private RelativeLayout cardFull;
    private RelativeLayout cardTrial;

    private ImageView imgLoading;
    private Animation animation;

    private TextView tvPremiumPeriod;
    private TextView tvPremiumPrice;

    private TextView tvPremiumDisclaimer;
    private TextView tvTrialDisclaimer;

    private RelativeLayout btnContinue;
    private ImageView btnClose;

    private BillingManager manager;

    private SkuDetails trialSku;
    private SkuDetails fullSku;

    private boolean isTrial = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_billing);

        Window window = getWindow();
        window.setStatusBarColor(MaterialColors.getColor(this, R.attr.billing_status_bar, R.attr.colorAccent));

        handler = new Handler(Looper.getMainLooper());

        imgLoading = findViewById(R.id.img_loading);
        animation = rotate(imgLoading);

        manager = BillingManager.get(this);

        btnClose = findViewById(R.id.btn_close);
        btnContinue = findViewById(R.id.btn_continue);

        cardFull = findViewById(R.id.card_full);
        cardTrial = findViewById(R.id.card_trial);

        tvPremiumPeriod = findViewById(R.id.txt_premium_period);
        tvPremiumPrice = findViewById(R.id.txt_premium_price);

        tvPremiumDisclaimer = findViewById(R.id.txt_premium_disclaimer);
        tvTrialDisclaimer = findViewById(R.id.txt_trial_disclaimer);

        adapter = new PageAdapter(this);
        pager = findViewById(R.id.pager);

        setupTabs();
        setButtons();
        loopFragments();
        retrieveSubs();
    }

    @Override
    public void onStop() {
        handler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void retrieveSubs() {
        RemoteConfig.fetchSubs(this, (trialSubId, premiumSubId) ->
                manager.retrieveSubs(trialSubId, premiumSubId, new ISkuListener() {
            @Override
            public void onResult(SkuDetails trial, SkuDetails full) {
                trialSku = trial;
                fullSku = full;

                runOnUiThread(() -> {

                    animation.cancel();
                    imgLoading.clearAnimation();
                    imgLoading.setVisibility(View.INVISIBLE);
                    findViewById(R.id.container).setVisibility(View.VISIBLE);

                    long trialPriceMicros = trial.getOriginalPriceAmountMicros();
                    long fullPriceMicros = full.getOriginalPriceAmountMicros();

                    double trialPrice = (double) trialPriceMicros / 1000000;
                    double fullPrice = (double) fullPriceMicros / 1000000;

                    Locale locale = Locale.getDefault();

                    String trialPriceString = trial.getPriceCurrencyCode() + String.format(locale, " %.2f", trialPrice);
                    String premiumPriceString = full.getPriceCurrencyCode() + String.format(locale, " %.2f", fullPrice);

                    Period periodTrial = Period.parse(trial.getSubscriptionPeriod());
                    Period periodTrialFree = Period.parse(trial.getFreeTrialPeriod());
                    Period periodPremium = Period.parse(full.getSubscriptionPeriod());

                    tvPremiumPrice.setText(premiumPriceString);

                    String premiumPriceWithSub;

                    Resources res = getResources();

                    if (periodPremium.years > 0) {
                        tvPremiumPeriod.setText(getString(R.string.txt_premium_period, res.getQuantityString(R.plurals.years, periodPremium.years, periodPremium.years)));
                        premiumPriceWithSub = full.getPriceCurrencyCode() + String.format(locale," %.2f", fullPrice / (12 * periodPremium.years));
                        tvPremiumDisclaimer.setText(getString(R.string.txt_premium_disclaimer,
                                res.getQuantityString(R.plurals.yearly, periodTrial.years, periodTrial.years),
                                premiumPriceString, premiumPriceWithSub, getString(R.string.month).toLowerCase()));
                    } else if (periodPremium.months > 0) {
                        tvPremiumPeriod.setText(getString(R.string.txt_premium_period, res.getQuantityString(R.plurals.months, periodPremium.months, periodPremium.months)));
                        premiumPriceWithSub = full.getPriceCurrencyCode() + String.format(locale," %.2f", fullPrice / (4.35 * periodPremium.months));
                        tvPremiumDisclaimer.setText(getString(R.string.txt_premium_disclaimer,
                                res.getQuantityString(R.plurals.monthly, periodTrial.months, periodTrial.months),
                                premiumPriceString, premiumPriceWithSub, getString(R.string.week).toLowerCase()));
                    } else if (periodPremium.weeks > 0) {
                        tvPremiumPeriod.setText(getString(R.string.txt_premium_period, res.getQuantityString(R.plurals.weeks, periodPremium.weeks, periodPremium.weeks)));
                        premiumPriceWithSub = full.getPriceCurrencyCode() + String.format(locale," %.2f", fullPrice * 4.35 / periodPremium.weeks);
                        tvPremiumDisclaimer.setText(getString(R.string.txt_premium_disclaimer,
                                res.getQuantityString(R.plurals.weekly, periodTrial.weeks, periodTrial.weeks),
                                premiumPriceString, premiumPriceWithSub, getString(R.string.month).toLowerCase()));
                    } else if (periodPremium.days > 0) {
                        tvPremiumPeriod.setText(getString(R.string.txt_premium_period, res.getQuantityString(R.plurals.days, periodPremium.days, periodPremium.days)));
                        premiumPriceWithSub = full.getPriceCurrencyCode() + String.format(locale," %.2f", fullPrice * 7 / periodPremium.days);
                        tvPremiumDisclaimer.setText(getString(R.string.txt_premium_disclaimer,
                                res.getQuantityString(R.plurals.daily, periodTrial.days, periodTrial.days),
                                premiumPriceString, premiumPriceWithSub, getString(R.string.week).toLowerCase()));
                    }

                    if (periodTrial.years > 0) {
                        tvTrialDisclaimer.setText(getString(R.string.txt_trial_disclaimer,
                                res.getQuantityString(R.plurals.days_free, periodTrialFree.days, periodTrialFree.days).toLowerCase(),
                                res.getQuantityString(R.plurals.yearly, periodTrial.years, periodTrial.years),
                                trialPriceString,
                                trial.getPriceCurrencyCode() + String.format(locale, " %.2f", trialPrice / (12 * periodTrial.years)),
                                getString(R.string.month).toLowerCase()));
                    } else if (periodTrial.months > 0) {
                        tvTrialDisclaimer.setText(getString(R.string.txt_trial_disclaimer,
                                res.getQuantityString(R.plurals.days_free, periodTrialFree.days, periodTrialFree.days).toLowerCase(),
                                res.getQuantityString(R.plurals.monthly, periodTrial.months, periodTrial.months),
                                trialPriceString,
                                trial.getPriceCurrencyCode() + String.format(locale, " %.2f", trialPrice / (4.35 * periodTrial.months)),
                                getString(R.string.week).toLowerCase()));
                    } else if (periodTrial.weeks > 0) {
                        tvTrialDisclaimer.setText(getString(R.string.txt_trial_disclaimer,
                                res.getQuantityString(R.plurals.days_free, periodTrialFree.days, periodTrialFree.days).toLowerCase(),
                                res.getQuantityString(R.plurals.weekly, periodTrial.weeks, periodTrial.weeks),
                                trialPriceString,
                                trial.getPriceCurrencyCode() + String.format(locale, " %.2f", trialPrice  * 4.35 / periodTrial.weeks),
                                getString(R.string.month).toLowerCase()));
                    } else if (periodTrial.days > 0) {
                        tvTrialDisclaimer.setText(getString(R.string.txt_trial_disclaimer,
                                res.getQuantityString(R.plurals.days_free, periodTrialFree.days, periodTrialFree.days).toLowerCase(),
                                res.getQuantityString(R.plurals.daily, periodTrial.days, periodTrial.days),
                                trialPriceString,
                                trial.getPriceCurrencyCode() + String.format(locale, " %.2f", trialPrice * 7 / periodTrial.days),
                                getString(R.string.week).toLowerCase()));
                    }

                });

            }

            @Override
            public void onFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                    BillingActivity.this.finish();
                    overridePendingTransition(R.anim.from_right, R.anim.to_left);
                });
            }
        }));
    }

    private void setupTabs() {
        pager.setAdapter(adapter);
        TabLayout tabs = findViewById(R.id.tabDots);
        tabs.setTabRippleColor(null);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {}).attach();
    }

    private void setButtons() {

        btnClose.setOnClickListener(v -> {

            final Dialog dialog = new Dialog(BillingActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_exit_trial);
            dialog.setCancelable(false);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            if (BillingManager.DIALOG_EXIT_ICONS != null) {
                LayoutInflater inflater = dialog.getLayoutInflater();
                int layoutId = getResources().getIdentifier(BillingManager.DIALOG_EXIT_ICONS, "layout", getPackageName());
                View icons = inflater.inflate(layoutId, null);
                ((ViewGroup) dialog.findViewById(R.id.dialog_icons)).addView(icons);
            }

            TextView tvTimer = dialog.findViewById(R.id.dialog_timer);

            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    counter -= 1;

                    long hours = TimeUnit.SECONDS.toHours(counter) % 24;
                    long minutes = TimeUnit.SECONDS.toMinutes(counter) % 60;
                    long seconds = TimeUnit.SECONDS.toSeconds(counter) % 60;

                    tvTimer.setText(getString(R.string.dialog_exit_timer, hours, minutes, seconds));
                    handler.postDelayed(this, 1000);
                }
            }, 1000);

            dialog.findViewById(R.id.dialog_button_close).setOnClickListener(v1 -> {
                handler.removeCallbacksAndMessages(null);
                dialog.dismiss();
                finish();
                overridePendingTransition(R.anim.from_right, R.anim.to_left);
            });

            dialog.findViewById(R.id.dialog_button_ok).setOnClickListener(v12 -> {
                handler.removeCallbacksAndMessages(null);
                dialog.dismiss();
                manager.launchPurchaseFlow(BillingActivity.this, trialSku, onPurchaseListener);
            });

        });
        btnContinue.setOnClickListener(v -> {
            if (trialSku == null || fullSku == null) {
                retrieveSubs();
                return;
            }
            if (isTrial)
                manager.launchPurchaseFlow(BillingActivity.this, trialSku, onPurchaseListener);
            else
                manager.launchPurchaseFlow(BillingActivity.this, fullSku, onPurchaseListener);
        });

        cardFull.setOnClickListener(v -> {
            cardFull.setSelected(true);
            cardTrial.setSelected(false);
            isTrial = false;
            tvTrialDisclaimer.setVisibility(View.INVISIBLE);
            tvPremiumDisclaimer.setVisibility(View.VISIBLE);

        });

        cardTrial.setOnClickListener(v -> {
            cardTrial.setSelected(true);
            cardFull.setSelected(false);
            isTrial = true;
            tvTrialDisclaimer.setVisibility(View.VISIBLE);
            tvPremiumDisclaimer.setVisibility(View.INVISIBLE);
        });

        cardTrial.setSelected(true);
        cardFull.setSelected(false);
    }

    private Animation rotate(View view) {
        RotateAnimation rotate = new RotateAnimation(0, 1800,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(4000);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setFillAfter(true);
        rotate.setFillBefore(true);
        view.startAnimation(rotate);
        return rotate;
    }

    private void loopFragments() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (isFinishing() || isDestroyed())
                    return;
                try {
                    if (pager.getCurrentItem() == adapter.getItemCount() - 1) {
                        pager.setCurrentItem(0);
                    } else {
                        pager.setCurrentItem(pager.getCurrentItem() + 1);
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    return;
                }
                handler.postDelayed(this, 2000);
            }
        }, 2000);
    }
}
