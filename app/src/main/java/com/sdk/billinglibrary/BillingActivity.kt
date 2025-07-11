package com.sdk.billinglibrary

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.sdk.billinglibrary.LocalConfig.didFirstBilling
import com.sdk.billinglibrary.databinding.ActivityBillingBinding
import com.sdk.billinglibrary.databinding.BillingFeatureBinding
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout


interface IOnPurchaseListener {
    fun onPurchaseDone()
    fun onPurchaseFail()
    fun onPurchaseCancelled()
    fun onError()
}

class BillingActivity : AppCompatActivity() {

    companion object {
        private const val INDEX_STRING = 0
        private const val INDEX_ICON = 1
        private const val INDEX_BASIC = 2
    }



    private lateinit var binding: ActivityBillingBinding

    private var animation: Animation? = null

    private var isTrial = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If we are subscribed - finish immediately
        if (Billing.getStatus() !== BillingStatus.NOT_SUBSCRIBED) {
            finish()
            return
        }

        // Set content + start loading animation
        binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val window = window

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                view.setBackgroundColor(Color.TRANSPARENT)
                insets
            }
        } else {
            // Older SDKs: use direct window call
            window.statusBarColor = Color.TRANSPARENT
        }

        binding.btnClose.setOnClickListener { finish() }

        animation = rotate(binding.imgLoading)

        setFeatures()
        setButtons()

        lifecycleScope.launch {

            try {

                // Wait for billing initialization (5 seconds)
                Log.d(Billing.LOG, "Activity: Waiting for billing initialization")
                withTimeout(5000) {
                    Billing.manager.initialized.await()
                }
                Log.d(Billing.LOG, "Activity: Initialized!")

                // Fill all the info
                runOnUiThread {
                    if (Billing.manager.subTrial == null || Billing.manager.subFull == null) {
                        Log.d(Billing.LOG, "Activity: Error. Product are null")
                        Toast.makeText(applicationContext, R.string.purchase_fail, Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        setupSubs()
                        didFirstBilling()
                    }
                }

            } catch (ex: TimeoutCancellationException) {
                // Billing initialization timed out - finish
                Log.d(Billing.LOG, "Activity: Billing initialization timed out!")
                Toast.makeText(applicationContext, R.string.purchase_fail, Toast.LENGTH_LONG).show()
                finish()
            }
        }

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.from_right, R.anim.to_left)
    }


    private fun setupSubs() {

        if (Billing.manager.subTrial == null || Billing.manager.subFull == null)
            return

        animation?.cancel()
        binding.imgLoading.clearAnimation()

        binding.imgLoading.visibility = View.INVISIBLE
        binding.container.visibility = View.VISIBLE

        val priceTrial = Billing.manager.subTrial!!
        val pricePremium = Billing.manager.subFull!!

        binding.txtTrialTitle.text = getString(R.string.txt_trial_title, priceTrial.getTrialPeriod())
        binding.txtTrialDescr.text = getString(
            R.string.txt_trial_descr,
            priceTrial.getPriceAndCurrency(),
            priceTrial.getSubscriptionPeriod()
        )
        binding.txtTrialDisclaimer.text = getString(
            R.string.txt_trial_disclaimer,
            priceTrial.getTrialPeriod(),
            priceTrial.getSubscriptionPeriod(),
            priceTrial.getPriceAndCurrency(),
            priceTrial.getTotalPriceAndCurrency(),
            priceTrial.getTotalPeriod()
        )
        binding.txtPremiumTitle.text =
            getString(R.string.txt_premium_title, pricePremium.getSubscriptionPeriod())
        binding.txtPremiumDescr.text = getString(
            R.string.txt_premium_descr,
            pricePremium.getTotalPriceAndCurrency(),
            pricePremium.getTotalPeriod()
        )
        binding.txtPremiumPrice.text = pricePremium.getPriceAndCurrency()
        binding.txtPremiumPricePeriod.text =
            getString(R.string.txt_premium_price_period, pricePremium.getSubscriptionPeriod())
        binding.txtPremiumDisclaimer.text = getString(
            R.string.txt_premium_disclaimer,
            pricePremium.getSubscriptionPeriod(),
            pricePremium.getPriceAndCurrency(),
            pricePremium.getTotalPriceAndCurrency(),
            pricePremium.getTotalPeriod()
        )

    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (Billing.manager.subTrial == null) {
            Billing.onDismiss?.invoke()
            finish()
            return
        }
        showExitDialog()
    }

    private fun setButtons() {

        val theme = theme


        binding.btnClose.isEnabled = true
        binding.btnClose.visibility = View.VISIBLE
        binding.btnClose.setOnClickListener { showExitDialog() }

        val typedValue = TypedValue()

        if (theme.resolveAttribute(R.attr.billing_button_text_color, typedValue, true)) {
            @ColorInt val color = typedValue.data
            binding.btnContinueText.setTextColor(color)
            binding.txtTrialVersion.setTextColor(color)
            binding.txtGreatPrice.setTextColor(color)
            binding.btnContinueArrow.setColorFilter(color)
        }

        val typedValue2 = TypedValue()

        var animation = true
        if (theme.resolveAttribute(R.attr.billing_button_animation, typedValue2, false)) {
            if (typedValue2.data == 0)
                animation = false
        }

        val view = findViewById<ShimmerFrameLayout>(R.id.txt_progress_container)
        if (animation)
            view.startShimmer()

        binding.btnContinue.setOnClickListener {
            if (Billing.manager.subTrial == null || Billing.manager.subFull == null) {
                finish()
                return@setOnClickListener
            }
            if (isTrial)
                Billing.manager.launchPurchaseFlow(this@BillingActivity, Billing.manager.subTrial)
            else
                Billing.manager.launchPurchaseFlow(this@BillingActivity, Billing.manager.subFull)
        }

        binding.cardFull.setOnClickListener {
            binding.cardFull.isSelected = true
            binding.cardTrial.isSelected = false
            isTrial = false
            binding.txtTrialDisclaimer.visibility = View.GONE
            binding.txtPremiumDisclaimer.visibility = View.VISIBLE
        }
        binding.cardTrial.setOnClickListener {
            binding.cardTrial.isSelected = true
            binding.cardFull.isSelected = false
            isTrial = true
            binding.txtTrialDisclaimer.visibility = View.VISIBLE
            binding.txtPremiumDisclaimer.visibility = View.GONE
        }
        binding.cardTrial.isSelected = true
        binding.cardFull.isSelected = false
    }

    private fun showExitDialog() {
        val typedValue = TypedValue()
        if (theme.resolveAttribute(R.attr.billing_show_dialog, typedValue, true)) {
            if (typedValue.data == 0) {
                Billing.onDismiss?.invoke()
                finish()
                return
            }
        }

        val dialog = ExitDialog(this)
        dialog.findViewById<View>(R.id.dialog_button_ok).setOnClickListener {
            dialog.dismiss()
            Billing.manager.launchPurchaseFlow(this@BillingActivity, Billing.manager.subTrial)
        }

        try {
            val price = Billing.manager.subTrial!!
            val tvDisclaimer = dialog.findViewById<TextView>(R.id.txt_dialog_disclaimer)
            tvDisclaimer.text = getString(
                R.string.dialog_disclaimer,
                price.getTrialPeriod(),
                price.getPriceAndCurrency(),
                price.getSubscriptionPeriod()
            )
            dialog.show()
        } catch (ignore: NullPointerException) {
            finish()
        }
    }

    private fun rotate(view: View?): Animation {
        val rotate = RotateAnimation(
            0f, 1800f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotate.duration = 4000
        rotate.interpolator = AccelerateDecelerateInterpolator()
        rotate.repeatCount = Animation.INFINITE
        rotate.fillAfter = true
        rotate.fillBefore = true
        view!!.startAnimation(rotate)
        return rotate
    }

    private fun setFeatures() {
        val attrs = intArrayOf(R.attr.billing_features)
        val a = obtainStyledAttributes(attrs)
        val id = a.getResourceId(0, 0)
        a.recycle()
        if (id == 0) return
        val features = getResources().obtainTypedArray(id)
        for (i in 0 until features.length()) {
            val featureId = features.getResourceId(i, 0)
            val feature = getResources().obtainTypedArray(featureId)
            val featureNameId = feature.getResourceId(INDEX_STRING, 0)
            val featureName = getString(featureNameId)
            val featureIcon = feature.getDrawable(INDEX_ICON)
            val featureBasic = feature.getBoolean(INDEX_BASIC, false)
            val item = BillingFeatureBinding.inflate(layoutInflater, binding.featuresContainer, false)
            item.txtFeatureName.text = featureName
            item.imgIcon.setImageDrawable(featureIcon)
            if (i < features.length() - 1) {
                item.imgBasic.setImageDrawable(
                    if (featureBasic)
                        AppCompatResources.getDrawable(this, R.drawable.billing_check)
                    else
                        AppCompatResources.getDrawable(this, R.drawable.billing_cancel)
                )
            }
            binding.featuresContainer.addView(item.root)
            feature.recycle()
        }
        features.recycle()
    }
}
