package com.sdk.billinglibrary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.ProductDetails;
import com.sdk.billinglibrary.interfaces.IOnPurchaseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BillingOfferActivity extends AppCompatActivity {

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

    private static final int INDEX_STRING = 0;
    private static final int INDEX_ICON = 1;

    private BillingManager mBillingManager;

    private View cardWeekly;
    private View cardTrial;
    private View cardLifetime;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private TextView tvTimer;
    private int mCounter = 900;

    private ProductDetails mWeeklySub;
    private ProductDetails mTrialSub;
    private ProductDetails mLifetimeSub;

    private int mChosenSub = 1;

    private TextView tvWeeklyFull;
    private TextView tvTrialFull;
    private TextView tvLifetimeFull;

    private TextView tvWeeklySale;
    private TextView tvTrialSale;
    private TextView tvLifetimeSale;

    private TextView tvDisclaimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_offer);

        mBillingManager = BillingManager.getInstance();

        tvWeeklyFull = findViewById(R.id.txt_weekly_full);
        tvTrialFull = findViewById(R.id.txt_trial_full);
        tvLifetimeFull = findViewById(R.id.txt_lifetime_full);

        tvWeeklySale = findViewById(R.id.txt_weekly_sale);
        tvTrialSale = findViewById(R.id.txt_trial_sale);
        tvLifetimeSale = findViewById(R.id.txt_lifetime_sale);

        setFeatures();
        setCards();
        setTimer();

        retrieveSubs();

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            ProductDetails sub = null;
            switch (mChosenSub) {
                case 0:
                    sub = mWeeklySub;
                    break;
                case 1:
                    sub = mTrialSub;
                    break;
                case 2:
                    sub = mLifetimeSub;
                    break;
            }

            if (sub != null) {
                mBillingManager.launchPurchaseFlow(this, sub,
                        sub.getSubscriptionOfferDetails().get(0).getOfferToken(), onPurchaseListener);
            }
        });

    }

    private void retrieveSubs() {

        RemoteConfig.fetchSubs(this, (isSuccessful) -> {

            String weeklySubId = RemoteConfig.getSubByKey(RemoteConfig.SUB_OFFER_WEEKLY);
            String trialSubId = RemoteConfig.getSubByKey(RemoteConfig.SUB_OFFER_TRIAL);
            String lifetimeSubId = RemoteConfig.getSubByKey(RemoteConfig.SUB_OFFER_LIFETIME);

            List<String> subIds = new ArrayList<>();
            subIds.add(weeklySubId);
            subIds.add(trialSubId);
            subIds.add(lifetimeSubId);;

            mBillingManager.retrieveSubs(subIds, (isSuccessful1, products) ->
                    runOnUiThread(() -> {
                        if (isSuccessful1 && products != null) {

                            for (ProductDetails product : products) {
                                if (product.getProductId().equals(weeklySubId))
                                    mWeeklySub = product;
                                else if (product.getProductId().equals(trialSubId))
                                    mTrialSub = product;
                                else if (product.getProductId().equals(lifetimeSubId))
                                    mLifetimeSub = product;
                            }

                            if (mWeeklySub == null || mTrialSub == null || mLifetimeSub == null) {
                                finish();
                                return;
                            }


                            tvWeeklyFull.setText(getString(R.string.offer_weekly_full, mWeeklySub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice()));
                            tvWeeklySale.setText(getString(R.string.offer_weekly_sale, mWeeklySub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice()));

                            tvTrialFull.setText(getString(R.string.offer_weekly_full, mTrialSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(2).getFormattedPrice()));
                            tvTrialSale.setText(getString(R.string.offer_trial_sale, mTrialSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice()));

                            tvLifetimeFull.setText(getString(R.string.offer_lifetime_full, mLifetimeSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice()));
                            tvLifetimeSale.setText(getString(R.string.offer_weekly_sale, mLifetimeSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice()));

                            updateDisclaimer();

                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Something went wrong...", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })
            );
        });
    }

    private void updateDisclaimer() {
        switch (mChosenSub) {
            case 0:
                if (mWeeklySub != null)
                    tvDisclaimer.setText(getString(R.string.offer_disclaimer_weekly,
                                    mWeeklySub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice(),
                                    mWeeklySub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice()));
                break;
            case 1:
                if (mTrialSub != null)
                    tvDisclaimer.setText(getString(R.string.offer_disclaimer_trial,
                            mTrialSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice(),
                            mTrialSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(2).getFormattedPrice()));
                break;
            case 2:
                if (mLifetimeSub != null)
                    tvDisclaimer.setText(getString(R.string.offer_disclaimer_lifetime,
                            mLifetimeSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice(),
                            mLifetimeSub.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(1).getFormattedPrice()));
                break;
            default:
                break;
        }
    }

    private void setTimer() {
        tvTimer = findViewById(R.id.txt_offer_timer);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCounter -= 1;
                if (mCounter == 0) {
                    finish();
                    return;
                }
                long minutes = TimeUnit.SECONDS.toMinutes(mCounter) % 60;
                long seconds = TimeUnit.SECONDS.toSeconds(mCounter) % 60;
                tvTimer.setText(getString(R.string.offer_timer, minutes, seconds));
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void setCards() {

        tvDisclaimer = findViewById(R.id.txt_offer_disclaimer);

        cardWeekly = findViewById(R.id.card_offer_weekly);
        cardTrial = findViewById(R.id.card_offer_trial);
        cardLifetime = findViewById(R.id.card_offer_lifetime);

        cardWeekly.setOnClickListener(v -> {
            mChosenSub = 0;
            cardWeekly.setSelected(true);
            cardTrial.setSelected(false);
            cardLifetime.setSelected(false);
            ((CardView) cardWeekly.findViewById(R.id.card_offer_weekly_card)).setCardElevation(0.f);
            ((CardView) cardTrial.findViewById(R.id.card_offer_trial_card)).setCardElevation(7.f);
            ((CardView) cardLifetime.findViewById(R.id.card_offer_lifetime_card)).setCardElevation(7.f);
            updateDisclaimer();
        });

        cardTrial.setOnClickListener(v -> {
            mChosenSub = 1;
            cardWeekly.setSelected(false);
            cardTrial.setSelected(true);
            cardLifetime.setSelected(false);
            ((CardView) cardWeekly.findViewById(R.id.card_offer_weekly_card)).setCardElevation(7.f);
            ((CardView) cardTrial.findViewById(R.id.card_offer_trial_card)).setCardElevation(0.f);
            ((CardView) cardLifetime.findViewById(R.id.card_offer_lifetime_card)).setCardElevation(7.f);
            updateDisclaimer();
        });

        cardLifetime.setOnClickListener(v -> {
            mChosenSub = 2;
            cardWeekly.setSelected(false);
            cardTrial.setSelected(false);
            cardLifetime.setSelected(true);
            ((CardView) cardWeekly.findViewById(R.id.card_offer_weekly_card)).setCardElevation(7.f);
            ((CardView) cardTrial.findViewById(R.id.card_offer_trial_card)).setCardElevation(7.f);
            ((CardView) cardLifetime.findViewById(R.id.card_offer_lifetime_card)).setCardElevation(0.f);
            updateDisclaimer();
        });

        cardTrial.setSelected(true);
        cardWeekly.setSelected(false);
        cardLifetime.setSelected(false);

    }

    private void setFeatures() {

        ViewGroup featuresContainer = findViewById(R.id.offer_features);
        TypedArray a = obtainStyledAttributes(new int[] { R.attr.billing_offer_features }) ;
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

            View item = getLayoutInflater().inflate(R.layout.offer_feature, featuresContainer, false);
            ((TextView) item.findViewById(R.id.txt_feature_name)).setText(featureName);
            ((ImageView) item.findViewById(R.id.img_feature_icon)).setImageDrawable(featureIcon);
            featuresContainer.addView(item);
            feature.recycle();
        }

        features.recycle();
    }
}