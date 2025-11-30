package com.aatmik.calculator.util

import android.content.Context
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.LayoutFeatureDiscoveryBinding

/**
 * Simple feature discovery overlay manager
 * Shows step-by-step tutorial highlights for important UI elements
 */
class FeatureDiscoveryManager(
    private val context: Context,
    private val rootView: ViewGroup
) {

    private var overlayView: View? = null
    private var currentStep = 0
    private val steps = mutableListOf<DiscoveryStep>()
    private var onComplete: (() -> Unit)? = null

    data class DiscoveryStep(
        val targetView: View,
        val title: String,
        val description: String,
        val spotlightRadius: Int = 150 // in dp
    )

    /**
     * Add a discovery step
     */
    fun addStep(targetView: View, title: String, description: String, radiusDp: Int = 150): FeatureDiscoveryManager {
        steps.add(DiscoveryStep(targetView, title, description, radiusDp))
        return this
    }

    /**
     * Set completion callback
     */
    fun onComplete(callback: () -> Unit): FeatureDiscoveryManager {
        onComplete = callback
        return this
    }

    /**
     * Start the discovery flow
     */
    fun start() {
        if (steps.isEmpty()) return

        currentStep = 0
        showStep(currentStep)

        AnalyticsManager.log("feature_discovery_started")
    }

    /**
     * Show a specific step
     */
    private fun showStep(stepIndex: Int) {
        if (stepIndex >= steps.size) {
            complete()
            return
        }

        val step = steps[stepIndex]

        // Remove previous overlay if exists
        removeOverlay()

        // Create overlay
        val binding = LayoutFeatureDiscoveryBinding.inflate(
            LayoutInflater.from(context),
            rootView,
            false
        )

        // Create custom overlay with spotlight
        val overlayBg = SpotlightView(context, step.targetView, dpToPx(step.spotlightRadius))
        overlayBg.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        binding.overlayBackground.visibility = View.GONE // Hide the default overlay
        binding.featureDiscoveryRoot.addView(overlayBg, 0) // Add custom overlay as first child

        // Update step info
        binding.tvStepIndicator.text = "${stepIndex + 1}/${steps.size}"
        binding.tvFeatureTitle.text = step.title
        binding.tvFeatureDescription.text = step.description

        // Update button text
        if (stepIndex == steps.size - 1) {
            binding.btnNext.text = "Got it!"
        } else {
            binding.btnNext.text = "Next"
        }

        // Button clicks
        binding.btnSkip.setOnClickListener {
            ButtonUtil.vibratePhone(context)
            complete()
            AnalyticsManager.log("feature_discovery_skipped", "step" to (stepIndex + 1).toString())
        }

        binding.btnNext.setOnClickListener {
            ButtonUtil.vibratePhone(context)
            currentStep++
            showStep(currentStep)
            AnalyticsManager.log("feature_discovery_next", "step" to stepIndex.toString())
        }

        // Add to root view
        rootView.addView(binding.root)
        overlayView = binding.root

        // Animate in
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * Complete the discovery flow
     */
    private fun complete() {
        removeOverlay()
        onComplete?.invoke()

        // Mark as completed in preferences
        PrefUtil.setFeatureDiscoveryCompleted(context, true)
        AnalyticsManager.log("feature_discovery_completed")
    }

    /**
     * Remove overlay
     */
    private fun removeOverlay() {
        overlayView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            overlayView = null
        }
    }

    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Custom view that draws spotlight effect
     */
    private inner class SpotlightView(
        context: Context,
        private val targetView: View,
        private val radius: Int
    ) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Draw semi-transparent overlay
            paint.color = Color.parseColor("#CC000000")
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // Get target view position
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)

            val rootLocation = IntArray(2)
            (parent as View).getLocationOnScreen(rootLocation)

            val centerX = location[0] - rootLocation[0] + targetView.width / 2f
            val centerY = location[1] - rootLocation[1] + targetView.height / 2f

            // Draw spotlight (clear circle)
            canvas.drawCircle(centerX, centerY, radius.toFloat(), eraserPaint)

            // Draw white ring around spotlight
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(centerX, centerY, radius.toFloat(), paint)
        }
    }

    companion object {
        /**
         * Check if discovery should be shown
         */
        fun shouldShow(context: Context): Boolean {
            return !PrefUtil.isFeatureDiscoveryCompleted(context)
        }
    }
}