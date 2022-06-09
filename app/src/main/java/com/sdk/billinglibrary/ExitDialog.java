package com.sdk.billinglibrary;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.utilityapps.adshelperlib.AdsHelper;

import java.util.concurrent.TimeUnit;

class ExitDialog extends Dialog {

    private final boolean mShowInterAfter;

    private final Resources mResources;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mCounter = 86400;

    public ExitDialog(@NonNull Activity activity, boolean showInter) {
        super(activity);

        mShowInterAfter = showInter;
        mResources = activity.getResources();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_exit_trial);
        setCancelable(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int layoutId = mResources.getIdentifier(
                "dialog_billing_icons",
                "layout",
                activity.getPackageName()
        );

        if (layoutId != 0) {
            View icons = getLayoutInflater().inflate(layoutId, null);
            ((ViewGroup) findViewById(R.id.dialog_icons)).addView(icons);
        }


        findViewById(R.id.dialog_button_close).setOnClickListener(v1 -> {
            mHandler.removeCallbacksAndMessages(null);
            dismiss();
            if (mShowInterAfter)
                AdsHelper.showInter(activity, true);
            activity.finish();
        });
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
}
