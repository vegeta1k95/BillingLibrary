package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;

import java.util.concurrent.TimeUnit;

class ExitDialog extends Dialog {

    private static final int INDEX_STRING = 0;
    private static final int INDEX_ICON = 1;
    private static final int INDEX_BASIC = 2;

    private final Resources mResources;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mCounter = 86400;

    public ExitDialog(@NonNull Activity activity) {
        super(activity);

        mResources = activity.getResources();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_exit_trial);
        setCancelable(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        if (theme.resolveAttribute(R.attr.billing_button_text_color, typedValue, true)) {
            @ColorInt int color = typedValue.data;
            ((AppCompatButton) findViewById(R.id.dialog_button_ok)).setTextColor(color);
        }

        findViewById(R.id.dialog_button_close).setOnClickListener(v1 -> {
            mHandler.removeCallbacksAndMessages(null);
            dismiss();
            if (Billing.mCallback != null)
                Billing.mCallback.onDismiss();
            activity.finish();
        });

        setFeatures();
    }

    @Override
    public void show() {
        super.show();

        TextView tvTimer = findViewById(R.id.dialog_timer);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCounter -= 1;

                long hours = TimeUnit.SECONDS.toHours(mCounter) % 24;
                long minutes = TimeUnit.SECONDS.toMinutes(mCounter) % 60;
                long seconds = TimeUnit.SECONDS.toSeconds(mCounter) % 60;

                tvTimer.setText(mResources.getString(R.string.dialog_exit_timer, hours, minutes, seconds));
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    private void setFeatures() {

        int[] attrs = new int[] { R.attr.billing_features };
        TypedArray a = getContext().obtainStyledAttributes(attrs);
        int id = a.getResourceId(0 , 0);

        a.recycle();

        if (id == 0)
            return;

        TypedArray features = getContext().getResources().obtainTypedArray(id);
        ViewGroup container = findViewById(R.id.dialog_features);

        for (int i = 0; i < features.length()-1; i++) {

            int featureId = features.getResourceId(i, 0);

            TypedArray feature = getContext().getResources().obtainTypedArray(featureId);

            int featureNameId = feature.getResourceId(INDEX_STRING, 0);
            String featureName = getContext().getString(featureNameId);
            Drawable featureIcon = feature.getDrawable(INDEX_ICON);
            boolean featureBasic = feature.getBoolean(INDEX_BASIC, false);

            if (featureBasic)
                continue;

            View item = getLayoutInflater().inflate(R.layout.dialog_feature, container, false);
            ((TextView) item.findViewById(R.id.txt_feature_name)).setText(featureName);
            ((ImageView) item.findViewById(R.id.img_icon)).setImageDrawable(featureIcon);
            container.addView(item);
            feature.recycle();
        }

        features.recycle();
    }
}
