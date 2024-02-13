package com.sdk.billinglibrary

import android.app.Activity
import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import java.util.concurrent.TimeUnit

internal class ExitDialog(activity: Activity) : Dialog(activity) {

    private val mResources: Resources
    private val mHandler = Handler(Looper.getMainLooper())
    private var mCounter = 86400

    init {
        mResources = activity.resources

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_exit_trial)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val typedValue = TypedValue()
        val theme = activity.theme

        if (theme.resolveAttribute(R.attr.billing_button_text_color, typedValue, true)) {
            @ColorInt val color = typedValue.data
            (findViewById<View>(R.id.dialog_button_ok) as AppCompatButton).setTextColor(color)
        }

        findViewById<View>(R.id.dialog_button_close).setOnClickListener { v1: View? ->
            mHandler.removeCallbacksAndMessages(null)
            dismiss()
            Billing.onDismiss?.invoke()
            activity.finish()
        }
        setFeatures()
    }

    override fun show() {
        super.show()
        val tvTimer = findViewById<TextView>(R.id.dialog_timer)
        mHandler.postDelayed(object : Runnable {
            override fun run() {
                mCounter -= 1
                val hours = TimeUnit.SECONDS.toHours(mCounter.toLong()) % 24
                val minutes = TimeUnit.SECONDS.toMinutes(mCounter.toLong()) % 60
                val seconds = TimeUnit.SECONDS.toSeconds(mCounter.toLong()) % 60
                tvTimer.text =
                    mResources.getString(R.string.dialog_exit_timer, hours, minutes, seconds)
                mHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    public override fun onStop() {
        mHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private fun setFeatures() {
        val attrs = intArrayOf(R.attr.billing_features)
        val a = context.obtainStyledAttributes(attrs)
        val id = a.getResourceId(0, 0)
        a.recycle()
        if (id == 0) return
        val features = context.resources.obtainTypedArray(id)
        val container = findViewById<ViewGroup>(R.id.dialog_features)
        for (i in 0 until features.length() - 1) {
            val featureId = features.getResourceId(i, 0)
            val feature = context.resources.obtainTypedArray(featureId)
            val featureNameId = feature.getResourceId(INDEX_STRING, 0)
            val featureName = context.getString(featureNameId)
            val featureIcon = feature.getDrawable(INDEX_ICON)
            val featureBasic = feature.getBoolean(INDEX_BASIC, false)
            if (featureBasic) continue
            val item = layoutInflater.inflate(R.layout.dialog_feature, container, false)
            (item.findViewById<View>(R.id.txt_feature_name) as TextView).text = featureName
            (item.findViewById<View>(R.id.img_icon) as ImageView).setImageDrawable(featureIcon)
            container.addView(item)
            feature.recycle()
        }
        features.recycle()
    }

    companion object {
        private const val INDEX_STRING = 0
        private const val INDEX_ICON = 1
        private const val INDEX_BASIC = 2
    }
}
