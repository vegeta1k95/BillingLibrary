package com.sdk.billinglibrary;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.android.billingclient.api.SkuDetails;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.FirebaseApp;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;
import com.sdk.billinglibrary.interfaces.ISkuListener;

public class BillingActivity extends AppCompatActivity {

    private final IOnPurchaseListener onPurchaseListener = new IOnPurchaseListener() {
        @Override
        public void onPurchaseDone() {
            Toast.makeText(getApplicationContext(), R.string.purchase_done, Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void onPurchaseFail() {
            Toast.makeText(getApplicationContext(), R.string.purchase_fail, Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void onPurchaseCancelled() {}

        @Override
        public void onError() {
            Toast.makeText(getApplicationContext(), R.string.purchase_fail, Toast.LENGTH_LONG).show();
            finish();
        }
    };

    private LinearLayout featuresContainer;

    private LinearLayout cardFull;
    private LinearLayout cardTrial;

    private ImageView imgLoading;
    private Animation animation;

    private TextView tvTrialTitle;
    private TextView tvTrialDescr;

    private TextView tvPremiumTitle;
    private TextView tvPremiumDescr;
    private TextView tvPremiumPrice;
    private TextView tvPremiumPricePeriod;

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
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
        //window.setStatusBarColor(MaterialColors.getColor(this, R.attr.billing_background_color, R.attr.colorAccent));

        featuresContainer = findViewById(R.id.features_container);

        imgLoading = findViewById(R.id.img_loading);
        animation = rotate(imgLoading);

        manager = BillingManager.getInstance();

        btnClose = findViewById(R.id.btn_close);
        btnContinue = findViewById(R.id.btn_continue);

        cardFull = findViewById(R.id.card_full);
        cardTrial = findViewById(R.id.card_trial);

        tvTrialTitle = findViewById(R.id.txt_trial_title);
        tvTrialDescr = findViewById(R.id.txt_trial_descr);

        tvPremiumTitle = findViewById(R.id.txt_premium_title);
        tvPremiumDescr = findViewById(R.id.txt_premium_descr);
        tvPremiumPrice = findViewById(R.id.txt_premium_price);
        tvPremiumPricePeriod = findViewById(R.id.txt_premium_price_period);

        tvPremiumDisclaimer = findViewById(R.id.txt_premium_disclaimer);
        tvTrialDisclaimer = findViewById(R.id.txt_trial_disclaimer);

        setFeatures();
        setButtons();
        retrieveSubs();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_right, R.anim.to_left);
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

                    Resources res = getResources();

                    Price priceTrial = new Price(res, trialSku);
                    Price pricePremium = new Price(res, fullSku);

                    tvTrialTitle.setText(getString(R.string.txt_trial_title, priceTrial.getTrialPeriod()));
                    tvTrialDescr.setText(getString(R.string.txt_trial_descr, priceTrial.getPriceAndCurrency(), priceTrial.getSubscriptionPeriod()));
                    tvTrialDisclaimer.setText(getString(R.string.txt_trial_disclaimer,
                            priceTrial.getTrialPeriod(),
                            priceTrial.getSubscriptionPeriod(),
                            priceTrial.getPriceAndCurrency(),
                            priceTrial.getTotalPriceAndCurrency(),
                            priceTrial.getTotalPeriod()));

                    tvPremiumTitle.setText(getString(R.string.txt_premium_title, pricePremium.getSubscriptionPeriod()));
                    tvPremiumDescr.setText(getString(R.string.txt_premium_descr, pricePremium.getTotalPriceAndCurrency(), pricePremium.getTotalPeriod()));

                    tvPremiumPrice.setText(pricePremium.getPriceAndCurrency());
                    tvPremiumPricePeriod.setText(getString(R.string.txt_premium_price_period, pricePremium.getSubscriptionPeriod()));

                    tvPremiumDisclaimer.setText(getString(R.string.txt_premium_disclaimer,
                            pricePremium.getSubscriptionPeriod(),
                            pricePremium.getPriceAndCurrency(),
                            pricePremium.getTotalPriceAndCurrency(),
                            pricePremium.getTotalPeriod()));

                });

            }

            @Override
            public void onFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                    BillingActivity.this.finish();
                });
            }
        }));
    }

    @Override
    public void onBackPressed() {
        final ExitDialog dialog = new ExitDialog(this);

        dialog.findViewById(R.id.dialog_button_ok).setOnClickListener(v12 -> {
            dialog.dismiss();
            manager.launchPurchaseFlow(BillingActivity.this, trialSku, onPurchaseListener);
        });

        Price price = new Price(getResources(), trialSku);

        TextView tvDisclaimer = dialog.findViewById(R.id.txt_dialog_disclaimer);
        tvDisclaimer.setText(getString(R.string.dialog_disclaimer,
                price.getTrialPeriod(),
                price.getPriceAndCurrency(),
                price.getSubscriptionPeriod()));
        dialog.show();
    }

    private void setButtons() {

        View btnTry = findViewById(R.id.btn_try);

        if (LocalConfig.isFirstTime()) {
            btnClose.setEnabled(false);
            btnClose.setVisibility(View.GONE);
            btnTry.setEnabled(true);
            btnTry.setVisibility(View.VISIBLE);
            btnTry.setOnClickListener(v -> {

                final ExitDialog dialog = new ExitDialog(this);

                dialog.findViewById(R.id.dialog_button_ok).setOnClickListener(v12 -> {
                    dialog.dismiss();
                    manager.launchPurchaseFlow(BillingActivity.this, trialSku, onPurchaseListener);
                });

                Price price = new Price(getResources(), trialSku);

                TextView tvDisclaimer = dialog.findViewById(R.id.txt_dialog_disclaimer);
                tvDisclaimer.setText(getString(R.string.dialog_disclaimer,
                        price.getTrialPeriod(),
                        price.getPriceAndCurrency(),
                        price.getSubscriptionPeriod()));
                dialog.show();

            });
        } else {
            btnClose.setEnabled(true);
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(v -> {

                final ExitDialog dialog = new ExitDialog(this);

                dialog.findViewById(R.id.dialog_button_ok).setOnClickListener(v12 -> {
                    dialog.dismiss();
                    manager.launchPurchaseFlow(BillingActivity.this, trialSku, onPurchaseListener);
                });

                Price price = new Price(getResources(), trialSku);

                TextView tvDisclaimer = dialog.findViewById(R.id.txt_dialog_disclaimer);
                tvDisclaimer.setText(getString(R.string.dialog_disclaimer,
                        price.getTrialPeriod(),
                        price.getPriceAndCurrency(),
                        price.getSubscriptionPeriod()));
                dialog.show();

            });

            btnTry.setEnabled(false);
            btnTry.setVisibility(View.GONE);
        }
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

    private static final int INDEX_STRING = 0;
    private static final int INDEX_ICON = 1;
    private static final int INDEX_BASIC = 2;

    private void setFeatures() {

        int[] attrs = new int[] { R.attr.billing_features };
        TypedArray a = obtainStyledAttributes(attrs);
        int id = a.getResourceId(0 , 0);

        a.recycle();

        if (id == 0)
            return;

        TypedArray features = getResources().obtainTypedArray(id);

        if (features == null)
            return;

        for (int i = 0; i < features.length(); i++) {

            int featureId = features.getResourceId(i, 0);

            TypedArray feature = getResources().obtainTypedArray(featureId);

            int featureNameId = feature.getResourceId(INDEX_STRING, 0);
            String featureName = getString(featureNameId);
            Drawable featureIcon = feature.getDrawable(INDEX_ICON);
            boolean featureBasic = feature.getBoolean(INDEX_BASIC, false);

            View item = getLayoutInflater().inflate(R.layout.billing_feature, featuresContainer, false);
            ((TextView) item.findViewById(R.id.txt_feature_name)).setText(featureName);
            ((ImageView) item.findViewById(R.id.img_icon)).setImageDrawable(featureIcon);
            ((ImageView) item.findViewById(R.id.img_basic)).setImageDrawable(
                    featureBasic ? AppCompatResources.getDrawable(this, R.drawable.check)
                            : AppCompatResources.getDrawable(this, R.drawable.basic_none)
            );
            featuresContainer.addView(item);
            feature.recycle();
        }

        features.recycle();
    }
}
