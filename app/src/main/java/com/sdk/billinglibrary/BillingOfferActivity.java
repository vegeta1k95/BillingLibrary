package com.sdk.billinglibrary;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class BillingOfferActivity extends AppCompatActivity {

    private static final int INDEX_STRING = 0;
    private static final int INDEX_ICON = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_offer);

        setFeatures();
    }

    private void setFeatures() {

        ViewGroup featuresContainer = findViewById(R.id.offer_features);

        int[] attrs = new int[] { R.attr.billing_features };
        TypedArray a = obtainStyledAttributes(attrs);
        int id = a.getResourceId(0 , 0);

        a.recycle();

        if (id == 0)
            return;

        TypedArray features = getResources().obtainTypedArray(id);

        if (features == null)
            return;

        for (int i = 4; i < features.length() - 1; i++) {

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