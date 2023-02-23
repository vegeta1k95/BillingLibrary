package com.sdk.billinglibrary;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
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

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.android.billingclient.api.ProductDetails;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;

import java.util.ArrayList;
import java.util.List;

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

    private ProductDetails trialSku;
    private ProductDetails fullSku;

    private boolean isTrial = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manager = BillingManager.getInstance();

        if (manager == null || Billing.getStatus() != Billing.Status.NOT_SUBSCRIBED) {
            finish();
            return;
        }

        setContentView(R.layout.activity_billing);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);

        featuresContainer = findViewById(R.id.features_container);

        imgLoading = findViewById(R.id.img_loading);
        animation = rotate(imgLoading);

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

        LocalConfig.didFirstBilling();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_right, R.anim.to_left);
    }

    private void retrieveSubs() {

        RemoteConfig.fetchSubs(this, (isSuccessful) -> {

            String trialSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_TRIAL);
            String premiumSubId = RemoteConfig.getSubByKey(RemoteConfig.KEY_PREMIUM);

            List<String> subIds = new ArrayList<>();
            subIds.add(trialSubId);
            subIds.add(premiumSubId);

            manager.retrieveSubs(subIds, (isSuccessful1, products) ->
                    runOnUiThread(() -> {
                        if (isSuccessful1 && products != null) {

                            for (ProductDetails product : products) {
                                if (product.getProductId().equals(trialSubId))
                                    trialSku = product;
                                else if (product.getProductId().equals(premiumSubId))
                                    fullSku = product;
                            }

                            animation.cancel();
                            imgLoading.clearAnimation();
                            imgLoading.setVisibility(View.INVISIBLE);
                            findViewById(R.id.container).setVisibility(View.VISIBLE);

                            Resources res = getResources();

                            Price priceTrial = new Price(res, trialSku.getSubscriptionOfferDetails().get(0));
                            Price pricePremium = new Price(res, fullSku.getSubscriptionOfferDetails().get(0));

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
                        } else {
                            Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })
            );
        });
    }

    @Override
    public void onBackPressed() {
        if (trialSku == null) {
            finish();
            return;
        }
        showExitDialog();
    }

    private void setButtons() {
        View.OnClickListener listener = v -> showExitDialog();
        View btnTry = findViewById(R.id.btn_try);

        if (LocalConfig.isFirstTimeBilling()) {
            btnClose.setEnabled(false);
            btnClose.setVisibility(View.GONE);
            btnTry.setEnabled(true);
            btnTry.setVisibility(View.VISIBLE);
            btnTry.setOnClickListener(listener);
        } else {
            btnClose.setEnabled(true);
            btnClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(listener);
            btnTry.setEnabled(false);
            btnTry.setVisibility(View.GONE);
        }

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        if (theme.resolveAttribute(R.attr.billing_button_text_color, typedValue, true)) {
            @ColorInt int color = typedValue.data;
            ((TextView) findViewById(R.id.btn_continue_text)).setTextColor(color);
            ((TextView) findViewById(R.id.txt_trial_version)).setTextColor(color);
            ((TextView) findViewById(R.id.txt_great_price)).setTextColor(color);
            ((ImageView) findViewById(R.id.btn_continue_arrow)).setColorFilter(color);
        }

        btnContinue.setOnClickListener(v -> {
            if (trialSku == null || fullSku == null) {
                retrieveSubs();
                return;
            }
            if (isTrial)
                manager.launchPurchaseFlow(BillingActivity.this, trialSku,
                        trialSku.getSubscriptionOfferDetails().get(0).getOfferToken(), onPurchaseListener);
            else
                manager.launchPurchaseFlow(BillingActivity.this, fullSku,
                        fullSku.getSubscriptionOfferDetails().get(0).getOfferToken(), onPurchaseListener);
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

    private void showExitDialog() {

        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(R.attr.billing_show_dialog, typedValue, true))
            if (typedValue.data == 0) {
                finish();
                return;
            }

        final ExitDialog dialog = new ExitDialog(this);

        dialog.findViewById(R.id.dialog_button_ok).setOnClickListener(v12 -> {
            dialog.dismiss();
            manager.launchPurchaseFlow(BillingActivity.this, trialSku,
                    trialSku.getSubscriptionOfferDetails().get(0).getOfferToken(), onPurchaseListener);
        });

        try {
            Price price = new Price(getResources(), trialSku.getSubscriptionOfferDetails().get(0));
            TextView tvDisclaimer = dialog.findViewById(R.id.txt_dialog_disclaimer);
            tvDisclaimer.setText(getString(R.string.dialog_disclaimer,
                    price.getTrialPeriod(),
                    price.getPriceAndCurrency(),
                    price.getSubscriptionPeriod()));
            dialog.show();
        } catch (NullPointerException ignore) {
            finish();
        }
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
                    featureBasic ? AppCompatResources.getDrawable(this, R.drawable.billing_check)
                            : AppCompatResources.getDrawable(this, R.drawable.billing_cancel)
            );
            featuresContainer.addView(item);
            feature.recycle();
        }

        features.recycle();
    }
}
