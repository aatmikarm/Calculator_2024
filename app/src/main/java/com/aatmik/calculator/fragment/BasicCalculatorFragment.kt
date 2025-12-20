package com.aatmik.calculator.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.activity.ContainerActivity
import com.aatmik.calculator.adapter.HistoryAdapter
import com.aatmik.calculator.adapter.HistoryBottomSheetAdapter
import com.aatmik.calculator.databinding.BottomSheetLayoutBinding
import com.aatmik.calculator.databinding.FragmentBasicCalculatorBinding
import com.aatmik.calculator.model.CalculationHistory
import com.aatmik.calculator.util.AdConfig
import com.aatmik.calculator.util.AdFrequencyManager
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ButtonUtil
import com.aatmik.calculator.util.ButtonUtil.addNumberValueToText
import com.aatmik.calculator.util.ButtonUtil.addOperatorValueToText
import com.aatmik.calculator.util.ButtonUtil.vibratePhone
import com.aatmik.calculator.util.CalculationUtil
import com.aatmik.calculator.util.FeatureDiscoveryManager
import com.aatmik.calculator.util.FirebaseConfigManager
import com.aatmik.calculator.util.HistoryManager
import com.aatmik.calculator.util.PrefUtil
import com.aatmik.calculator.util.RatingManager
import com.aatmik.calculator.util.SubscriptionManager
import com.aatmik.calculator.util.ThemeManager
import com.aatmik.calculator.util.UpdateManager
import com.aatmik.calculator.util.VoiceCalculatorManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

class BasicCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentBasicCalculatorBinding
    private var isPanelVisible = false
    private val calculationHistory = mutableListOf<CalculationHistory>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private var historyOverlayView: View? = null
    private var historyPanel: LinearLayout? = null
    private var isHistoryVisible = false
    private var isHistoryExpanded = false

    // Advanced calculator states
    private var isPowerMode = false
    private var baseValue: Double? = null
    private var isSecondMode = false
    private var isInDegreesMode = true

    // Memory functionality
    private var memoryValue: Double = 0.0

    // Undo/Redo functionality
    private val expressionHistory = mutableListOf<String>()
    private var historyIndex = -1

    private var isResultMode = false

    // Input validation
    private val inputHandler = Handler(Looper.getMainLooper())
    private var inputRunnable: Runnable? = null

    // Precision handling
    private val mathContext = MathContext(34, RoundingMode.HALF_UP)

    // Voice input
    private var voiceCalculatorManager: VoiceCalculatorManager? = null
    private var currentVoiceState = VoiceCalculatorManager.VoiceState.IDLE

    private var interstitialAd: InterstitialAd? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            voiceCalculatorManager?.startListening()
        } else {
            Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        var addedBC = false
    }

    enum class ValidationResult(val message: String) {
        VALID("Valid"),
        EMPTY("Empty expression"),
        CONSECUTIVE_OPERATORS("Consecutive operators"),
        UNBALANCED_PARENTHESES("Unbalanced parentheses"),
        INCOMPLETE("Incomplete expression"),
        INVALID_CHARS("Invalid characters")
    }

    sealed class CalculationResult {
        data class Success(val value: Double) : CalculationResult()
        data class Error(val message: String) : CalculationResult()
    }

    enum class ErrorType {
        CALCULATION, SYNTAX, OVERFLOW, DOMAIN
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBasicCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TEMPORARY: Reset feature discovery for testing - REMOVE THIS LATER
        //PrefUtil.resetFeatureDiscovery(requireContext())

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()
        setupButtons()
        historyView()
        restoreMemoryState()
        setupVoiceInput()
        setupEditableInput()
        preloadInterstitialAd()

        // Show feature discovery on first launch (after a small delay)
        binding.root.postDelayed({
            showFeatureDiscovery()
        }, 500) // 500ms delay to let everything load

        RatingManager.trackCalculatorUsage(requireContext(), "Basic Calculator ${System.currentTimeMillis()}")

    }

    // Replace setupEditableInput() method:
    private fun setupEditableInput() {
        // Disable system keyboard
        binding.tvSecondaryBC.showSoftInputOnFocus = false

        // Make both fields copyable
        binding.tvSecondaryBC.setTextIsSelectable(true)
        binding.tvPrimaryBC.setTextIsSelectable(true)

        binding.tvSecondaryBC.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // When user types, switch to input mode
                if (!isResultMode) {
                    switchToInputMode()
                }
                calculateLiveResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun preloadInterstitialAd() {
        if (!AdConfig.areAdsEnabled()) {
            return
        }

        if (AdConfig.getInterstitialAdId().isEmpty()) {
            return
        }

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireContext(),
            AdConfig.getInterstitialAdId(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("BasicCalcInterstitial", "Failed to load: ${adError.message}")
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("BasicCalcInterstitial", "Interstitial ad loaded")
                    interstitialAd = ad
                }
            }
        )
    }



    private fun showInterstitialIfNeeded() {
        if (!AdConfig.areAdsEnabled()) {
            return
        }

        // **ADD THIS: Check Remote Config**
        if (!FirebaseConfigManager.shouldShowInterstitialAds()) {
            Log.d("BasicCalcInterstitial", "Interstitial ads disabled via Remote Config")
            return
        }

        if (!AdFrequencyManager.shouldShowBasicCalculatorInterstitial(requireContext())) {
            return
        }

        interstitialAd?.let { ad ->
            ad.show(requireActivity())
            Log.d("BasicCalcInterstitial", "Showing interstitial ad")
            AnalyticsManager.log("interstitial_shown", "location" to "basic_calculator")
            preloadInterstitialAd()
        } ?: run {
            Log.d("BasicCalcInterstitial", "Ad not loaded yet")
            preloadInterstitialAd()
        }
    }

    private fun switchToInputMode() {
        binding.tvSecondaryBC.animate()
            .textSize(45f)
            .alpha(1f)
            .setDuration(150)
            .start()

        binding.tvPrimaryBC.animate()
            .textSize(35f)
            .alpha(0.7f)
            .setDuration(150)
            .start()
    }

    private fun switchToResultMode() {
        binding.tvSecondaryBC.animate()
            .textSize(35f)
            .alpha(0.7f)
            .setDuration(200)
            .start()

        binding.tvPrimaryBC.animate()
            .textSize(45f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun calculateLiveResult() {
        val expression = binding.tvSecondaryBC.text.toString()

        if (expression.isEmpty()) {
            binding.tvPrimaryBC.text = ""
            return
        }

        // FIX: Add × and ÷ to trimEnd
        val cleanExpression = expression.trimEnd('+', '-', '*', '/', '^', '×', '÷')

        if (cleanExpression.isEmpty()) {
            binding.tvPrimaryBC.text = ""
            return
        }

        val validation = validateExpressionRealTime(cleanExpression)  // ✅ VALIDATE CLEAN

        if (validation == ValidationResult.VALID) {
            val result = safeEvaluate(cleanExpression)  // ✅ EVALUATE CLEAN
            if (result is CalculationResult.Success) {
                val formatted = smartFormatResult(result.value)
                binding.tvPrimaryBC.text = formatWithCommas(formatted)
            } else {
                binding.tvPrimaryBC.text = ""
            }
        } else {
            binding.tvPrimaryBC.text = ""
        }
    }

    private fun historyView() {
        recyclerView = binding.rvHistory
        historyAdapter = HistoryAdapter(calculationHistory) { historyItem ->
            addToExpressionHistory(binding.tvSecondaryBC.text.toString())
            binding.tvSecondaryBC.setText(historyItem.expression)
            AnalyticsManager.log("calculation_history_used")
        }

        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        val arrowView = binding.swipeToExplore.findViewById<ImageView>(R.id.arrowSwipe)
        ObjectAnimator.ofFloat(arrowView, "translationX", 0f, 10f, 0f).apply {
            duration = 1000
            repeatCount = 9
            repeatMode = ObjectAnimator.RESTART
            start()
        }

        binding.historyButton.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            if (isHistoryVisible) {
                hideHistoryPanel()
            } else {
                showHistoryHalfScreen()
            }
        }

        binding.menuButton.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            showBottomSheet()
        }

        binding.swipeToExplore.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            (requireActivity() as? ContainerActivity)?.navigateToPage(
                ContainerActivity.PAGE_ALL_CALCULATORS,
                true
            )
        }
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)

        bottomSheetBinding.btnTheme.visibility = View.VISIBLE

        if (SubscriptionManager.isPremium()) {
            bottomSheetBinding.removeAdsText.text = "Premium Active ✓"
        }

        bottomSheetBinding.rateApp.setOnClickListener {
            rateApp()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnShareApp.setOnClickListener {
            shareApp()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnRemoveAds.setOnClickListener {
            removeAds()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnGetUpdate.setOnClickListener {
            UpdateManager.checkForUpdatesManually(requireActivity())
            AnalyticsManager.log("update_checked")
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnTheme.setOnClickListener {
            bottomSheetDialog.dismiss()
            showThemeSelector()
        }

        bottomSheetBinding.btnCustomerSupport.setOnClickListener {
            openCustomerSupport()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()

        AnalyticsManager.log("menu_opened")
    }

    private fun rateApp() {
        val appPackageName = "com.aatmik.calculator"
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName&showRating=true")
            )
        )
        AnalyticsManager.log("rate_app_clicked")
    }

    private fun removeAds() {
        if (SubscriptionManager.isPremium()) {
            Toast.makeText(requireContext(), "You're already a Premium user! 🎉", Toast.LENGTH_SHORT).show()
            AnalyticsManager.log("premium_already_active")
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Go Premium")
            .setMessage("Remove all ads and unlock all features!\n\n✓ No Banner Ads\n✓ No Interstitial Ads\n✓ All Features Unlocked\n✓ Works on all your devices")
            .setPositiveButton("Subscribe") { _, _ ->
                SubscriptionManager.startSubscriptionPurchase(requireActivity()) { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        AnalyticsManager.log("remove_ads_clicked")
    }

    private fun showFeatureDiscovery() {
        // Check if should show (first time only)
        if (!FeatureDiscoveryManager.shouldShow(requireContext())) {
            return
        }

        // Wait for views to be laid out
        binding.root.post {
            val manager = FeatureDiscoveryManager(
                requireContext(),
                requireActivity().findViewById(android.R.id.content)
            )
            manager.addStep(
                targetView = binding.menuButton,
                title = "Menu",
                description = "Change Themes, Customer Support, Get App Updates and more!",
                radiusDp = 50
            )
            manager.addStep(
                targetView = binding.historyButton,
                title = "History",
                description = "See all your calculations at one place, Reuse, Share, & Add a Note if you want",
                radiusDp = 80
            )
            manager.addStep(
                targetView = binding.swipeToExplore,
                title = "Explore More",
                description = "Swipe to Explore 100+ calculators!",
                radiusDp = 120
            )

            // Only add voice input step if it's visible
            val voiceButtonVisibility = binding.voiceInputButton.visibility
            Log.d("FeatureDiscovery", "Voice button visibility: $voiceButtonVisibility (VISIBLE=0, INVISIBLE=4, GONE=8)")

            if (binding.voiceInputButton.visibility == View.VISIBLE) {
                manager.addStep(
                    targetView = binding.voiceInputButton,
                    title = "Voice Input",
                    description = "Click Speak and Say 1 + 2 and see the Magic",
                    radiusDp = 80
                )
            }
            manager.addStep(
                targetView = binding.toggleArrow,
                title = "Scientific Mode",
                description = "Use Advanced Functions like sin, cos, tan, e, deg, root, power, log, pi, memory!",
                radiusDp = 70
            )
                .onComplete {
                    // Show a welcome toast
                    Toast.makeText(
                        requireContext(),
                        "You're all set! Start calculating! 🎉",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            manager.start()
        }
    }

    private fun shareApp() {
        val appPackageName = "com.aatmik.calculator"
        val appName = "Calculator App"
        val playStoreLink = "https://play.google.com/store/apps/details?id=$appPackageName"

        val shareMessage = """
        Hey! Check out this amazing calculator app I've been using.
        
        $appName - All-in-one calculator with multiple features including basic calculations, unit conversions, financial calculators, and much more!
        
        Download it here: $playStoreLink
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out $appName")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        try {
            startActivity(Intent.createChooser(shareIntent, "Share via"))
            AnalyticsManager.logAppShared("system_share")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCustomerSupport() {
        val supportEmail = "allinonecalculatorapp@gmail.com"
        val subject = "Calculator App Support Request"
        val body = """
        Dear Support Team,
        
        I need assistance with the Calculator App.
        
        Device Information:
        - App Version: ${getAppVersion()}
        - Android Version: ${Build.VERSION.RELEASE}
        - Device Model: ${Build.MODEL}
        - Device Manufacturer: ${Build.MANUFACTURER}
        
        Issue Description:
        [Please describe your issue here]
        
        Best regards,
        [Your name]
    """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
            AnalyticsManager.log("customer_support_opened")
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "No email app found. Please send an email to: $supportEmail",
                Toast.LENGTH_LONG
            ).show()

            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Support Email", supportEmail)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), "Email address copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private fun showThemeSelector() {
        val options = arrayOf(
            "Light Mode",
            "Dark Mode",
            "Follow System",
            "Default (Orange)",
            "Red Theme",
            "Green Theme",
            "Blue Theme",
            "Purple Theme",
            "Pink Theme"
        )

        val currentTheme = ThemeManager.getSavedTheme(requireContext())
        val currentSelection = when (currentTheme) {
            ThemeManager.THEME_LIGHT -> 0
            ThemeManager.THEME_DARK -> 1
            ThemeManager.THEME_SYSTEM -> 2
            ThemeManager.THEME_DEFAULT -> 3
            ThemeManager.THEME_RED -> 4
            ThemeManager.THEME_GREEN -> 5
            ThemeManager.THEME_BLUE -> 6
            ThemeManager.THEME_PURPLE -> 7
            ThemeManager.THEME_PINK -> 8
            else -> 3
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                val selectedTheme = when (which) {
                    0 -> ThemeManager.THEME_LIGHT
                    1 -> ThemeManager.THEME_DARK
                    2 -> ThemeManager.THEME_SYSTEM
                    3 -> ThemeManager.THEME_DEFAULT
                    4 -> ThemeManager.THEME_RED
                    5 -> ThemeManager.THEME_GREEN
                    6 -> ThemeManager.THEME_BLUE
                    7 -> ThemeManager.THEME_PURPLE
                    8 -> ThemeManager.THEME_PINK
                    else -> ThemeManager.THEME_DEFAULT
                }

                val themeName = options[which]
                ThemeManager.saveTheme(requireContext(), selectedTheme)
                AnalyticsManager.logThemeChanged(themeName)
                requireActivity().recreate()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHistoryHalfScreen() {
        if (isHistoryVisible) return

        val parentView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        val historyView = layoutInflater.inflate(R.layout.sliding_history_panel, parentView, false)

        val historyPanel = historyView.findViewById<LinearLayout>(R.id.historyPanel)
        val swipeHandleArea = historyView.findViewById<LinearLayout>(R.id.swipeHandleArea)
        val rvHistoryList = historyView.findViewById<RecyclerView>(R.id.rvHistoryList)
        val emptyStateLayout = historyView.findViewById<LinearLayout>(R.id.emptyStateLayout)
        val btnClearHistory = historyView.findViewById<ImageView>(R.id.btnClearHistory)
        val btnCloseHistory = historyView.findViewById<ImageView>(R.id.btnCloseHistory)
        val overlay = historyView.findViewById<FrameLayout>(R.id.historyOverlay)

        val historyList = HistoryManager.getHistory(requireContext())

        if (historyList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            rvHistoryList.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            rvHistoryList.visibility = View.VISIBLE
        }

        val adapter = HistoryBottomSheetAdapter(
            historyList,
            onReuse = { result ->
                animateResultPopulation(result)
                hideHistoryPanel()
            },
            onDelete = { position ->
                showDeleteConfirmationDialog(position) {
                    refreshHistoryInOverlay(rvHistoryList, emptyStateLayout)
                }
            }
        )

        rvHistoryList.adapter = adapter
        rvHistoryList.layoutManager = LinearLayoutManager(requireContext())

        btnClearHistory.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            showClearAllConfirmationDialog {
                refreshHistoryInOverlay(rvHistoryList, emptyStateLayout)
            }
        }

        btnCloseHistory.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            hideHistoryPanel()
        }

        overlay.setOnClickListener {
            hideHistoryPanel()
        }

        historyPanel.setOnClickListener { }

        setupPanelSwipeGesture(swipeHandleArea, historyPanel)

        historyPanel.translationY = -historyPanel.height.toFloat()
        parentView.addView(historyView)
        historyOverlayView = historyView
        this.historyPanel = historyPanel
        isHistoryVisible = true
        isHistoryExpanded = false

        overlay.alpha = 0f

        historyPanel.post {
            historyPanel.animate()
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            overlay.animate()
                .alpha(1f)
                .setDuration(350)
                .start()
        }

        AnalyticsManager.log("history_half_screen_opened")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPanelSwipeGesture(handleArea: View, panel: LinearLayout) {
        var startY = 0f
        var startTranslationY = 0f

        handleArea.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    startTranslationY = panel.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - startY
                    val newTranslationY = startTranslationY + deltaY

                    if (newTranslationY <= 0) {
                        panel.translationY = newTranslationY
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - startY
                    val threshold = 100f

                    if (deltaY < -threshold) {
                        panel.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .start()
                    } else if (deltaY > threshold) {
                        hideHistoryPanel()
                    } else {
                        panel.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun hideHistoryPanel() {
        if (!isHistoryVisible || historyOverlayView == null) return

        val view = historyOverlayView!!
        val panel = historyPanel!!
        val overlay = view.findViewById<FrameLayout>(R.id.historyOverlay)
        val parentView = view.parent as? ViewGroup

        panel.animate()
            .translationY(-panel.height.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        overlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                parentView?.removeView(view)
                historyOverlayView = null
                historyPanel = null
                isHistoryVisible = false
                isHistoryExpanded = false
            }
            .start()

        AnalyticsManager.log("history_panel_closed")
    }

    private fun animateResultPopulation(result: String) {
        ButtonUtil.vibratePhone(requireContext())

        binding.tvSecondaryBC.setText(result)
        binding.tvSecondaryBC.setSelection(result.length)

        binding.tvSecondaryBC.apply {
            scaleX = 0.7f
            scaleY = 0.7f
            alpha = 0.5f

            animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(1f)
                .setDuration(150)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }

        AnalyticsManager.log("history_result_populated", "result" to result)
    }

    private fun showDeleteConfirmationDialog(position: Int, onDeleted: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Calculation")
            .setMessage("Are you sure you want to delete this calculation?")
            .setPositiveButton("Delete") { _, _ ->
                HistoryManager.deleteHistoryItem(requireContext(), position)
                onDeleted()
                Toast.makeText(requireContext(), "Calculation deleted", Toast.LENGTH_SHORT).show()
                AnalyticsManager.log("history_item_deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearAllConfirmationDialog(onCleared: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear All History")
            .setMessage("Are you sure you want to delete all calculation history?")
            .setPositiveButton("Clear All") { _, _ ->
                HistoryManager.clearHistory(requireContext())
                onCleared()
                Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
                AnalyticsManager.log("history_cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshHistoryInOverlay(recyclerView: RecyclerView, emptyState: LinearLayout) {
        val historyList = HistoryManager.getHistory(requireContext())

        if (historyList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            (recyclerView.adapter as? HistoryBottomSheetAdapter)?.updateHistory(historyList)
        }
    }

    override fun onResume() {
        super.onResume()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isHistoryVisible) {
                hideHistoryPanel()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupVoiceInput() {
        // **CHECK: If voice input is enabled in Remote Config**
        if (!FirebaseConfigManager.isVoiceInputEnabled()) {
            // Hide the entire voice input CardView and info button
            binding.voiceInputButton.visibility = View.GONE
            binding.voiceInfoButton.visibility = View.GONE
            Log.d("BasicCalculator", "Voice input disabled via Remote Config")
            return
        }

        // Voice input is enabled, show buttons
        binding.voiceInputButton.visibility = View.VISIBLE
        binding.voiceInfoButton.visibility = View.VISIBLE

        voiceCalculatorManager = VoiceCalculatorManager(
            context = requireContext(),
            onStateChanged = { state ->
                currentVoiceState = state
                updateVoiceButtonUI(state)
            },
            onResultReceived = { expression, spokenText ->
                binding.tvSecondaryBC.setText(expression)
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                updateVoiceButtonUI(VoiceCalculatorManager.VoiceState.IDLE)
            }
        )

        binding.voiceInputButton.setOnClickListener {
            vibratePhone(requireContext())

            when (currentVoiceState) {
                VoiceCalculatorManager.VoiceState.IDLE -> {
                    if (ContextCompat.checkSelfPermission(requireContext(),
                            android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voiceCalculatorManager?.startListening()
                    } else {
                        requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
                VoiceCalculatorManager.VoiceState.LISTENING -> {
                    voiceCalculatorManager?.stopListening()
                }
                else -> {}
            }
        }

        binding.voiceInfoButton.setOnClickListener {
            vibratePhone(requireContext())
            showVoiceInputGuide()
        }
    }

    private fun showVoiceInputGuide() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)

        val message = """
        🎤 VOICE INPUT GUIDE
        
        Tap the "Speak" button and say your calculation naturally!
        
        ━━━━━━━━━━━━━━━━━━━━━━
        
        📊 NUMBERS
        • Say: "ten", "twenty five", "one hundred"
        • Examples:
          - "twenty five" → 25
          - "one hundred fifty" → 150
          - "three point five" → 3.5
        
        ━━━━━━━━━━━━━━━━━━━━━━
        
        ➕ OPERATORS
        • Addition: "plus", "add"
        • Subtraction: "minus", "subtract"
        • Multiplication: "times", "multiply"
        • Division: "divide", "divided by"
        • Percentage: "percent"
        • Decimal: "point", "dot"
        
        ━━━━━━━━━━━━━━━━━━━━━━
        
        💬 EXAMPLE PHRASES
        
        ✓ "twenty five plus fifty five"
          → 25+55 = 80
        
        ✓ "one hundred divided by four"
          → 100/4 = 25
        
        ✓ "three point five times two"
          → 3.5*2 = 7
        
        ✓ "fifty percent of two hundred"
          → 50%*200 = 100
        
        ━━━━━━━━━━━━━━━━━━━━━━
        
        💡 TIPS
        • Speak clearly and at normal speed
        • The result will appear automatically
        • You can edit the expression if needed
        • Works with all basic calculations
        
        ━━━━━━━━━━━━━━━━━━━━━━
        
        Need help? Just say your math problem naturally and let the app do the rest! 🚀
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Voice Input Guide")
            .setMessage(message)
            .setPositiveButton("Got it!") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Try Now") { dialog, _ ->
                dialog.dismiss()
                // Auto-trigger voice input
                binding.voiceInputButton.performClick()
            }
            .create()
            .show()

        AnalyticsManager.log("voice_guide_opened")
    }

    private fun updateVoiceButtonUI(state: VoiceCalculatorManager.VoiceState) {
        binding.apply {
            when (state) {
                VoiceCalculatorManager.VoiceState.IDLE -> {
                    voiceStatusText.text = "Speak"
                }
                VoiceCalculatorManager.VoiceState.LISTENING -> {
                    voiceStatusText.text = "Listening..."
                }
                VoiceCalculatorManager.VoiceState.PROCESSING -> {
                    voiceStatusText.text = "Processing..."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isHistoryVisible) {
            (historyOverlayView?.parent as? ViewGroup)?.removeView(historyOverlayView)
            historyOverlayView = null
            historyPanel = null
            isHistoryVisible = false
        }
        inputRunnable?.let { inputHandler.removeCallbacks(it) }
        voiceCalculatorManager?.cleanup()
        voiceCalculatorManager = null
        interstitialAd = null
    }

    private fun setupUI() {
        toggleBarLogic()
        clearError()
    }

    private fun setupButtons() {
        setupBasicButtons()
        setupScientificButtons()
        setupAdvancedButtons()
        setupMemoryButtons()
        setupControlButtons()
    }

    private fun addNewCalculationHistory(expression: String, result: String) {
        val newHistoryItem = CalculationHistory(expression, result)
        historyAdapter.addHistoryItem(newHistoryItem)
        recyclerView.scrollToPosition(historyAdapter.itemCount - 1)
    }

    private fun setupBasicButtons() {
        binding.apply {
            // Number buttons - INSERT AT CURSOR
            bt0BC.setOnClickListener { insertAtCursor("0") }
            bt1BC.setOnClickListener { insertAtCursor("1") }
            bt2BC.setOnClickListener { insertAtCursor("2") }
            bt3BC.setOnClickListener { insertAtCursor("3") }
            bt4BC.setOnClickListener { insertAtCursor("4") }
            bt5BC.setOnClickListener { insertAtCursor("5") }
            bt6BC.setOnClickListener { insertAtCursor("6") }
            bt7BC.setOnClickListener { insertAtCursor("7") }
            bt8BC.setOnClickListener { insertAtCursor("8") }
            bt9BC.setOnClickListener { insertAtCursor("9") }

            btBracketOpenBC.setOnClickListener {
                vibratePhone(requireContext())
                val currentText = tvSecondaryBC.text.toString()
                val cursorPosition = tvSecondaryBC.selectionStart
                val openCount = currentText.count { it == '(' }
                val closeCount = currentText.count { it == ')' }

                val bracketToAdd = if (openCount > closeCount &&
                    currentText.isNotEmpty() &&
                    cursorPosition > 0 &&
                    currentText[cursorPosition - 1] !in listOf('(', '+', '-', '*', '/', '×', '÷')) {
                    ")"
                } else {
                    "("
                }

                addToExpressionHistory(currentText)

                val newText = currentText.substring(0, cursorPosition) +
                        bracketToAdd +
                        currentText.substring(cursorPosition)

                tvSecondaryBC.setText(newText)
                tvSecondaryBC.setSelection(cursorPosition + 1)
            }

            btPercentageBC.setOnClickListener {
                vibratePhone(requireContext())
                handlePercentage()
            }

            // Operator buttons - INSERT AT CURSOR
            btAdditionBC.setOnClickListener { insertAtCursor("+") }
            btSubtractionBC.setOnClickListener { insertAtCursor("-") }
            btMultiplicationBC.setOnClickListener { insertAtCursor("×") }
            btDivisionBC.setOnClickListener { insertAtCursor("÷") }

            btDotBC.setOnClickListener {
                vibratePhone(requireContext())
                val currentText = tvSecondaryBC.text.toString()
                val lastNumber = getLastNumber(currentText)
                if (!lastNumber.contains(".")) {
                    insertAtCursor(".")
                }
            }

            btACBC.setOnClickListener {
                vibratePhone(requireContext())
                addToExpressionHistory(tvSecondaryBC.text.toString())
                tvSecondaryBC.text.clear()
                tvPrimaryBC.text = ""
                binding.tvSecondaryBC.textSize = 45f  // Reset to input mode
                binding.tvPrimaryBC.textSize = 35f
                addedBC = false
                clearError()
                resetCalculatorState()
                AnalyticsManager.log("calculator_cleared")
            }

            btDeleteBC.setOnClickListener {
                vibratePhone(requireContext())
                val currentText = tvSecondaryBC.text.toString()
                val cursorPosition = binding.tvSecondaryBC.selectionStart

                if (currentText.isNotEmpty() && cursorPosition > 0) {
                    addToExpressionHistory(currentText)

                    // Delete character BEFORE cursor
                    val newText = currentText.substring(0, cursorPosition - 1) +
                            currentText.substring(cursorPosition)

                    tvSecondaryBC.setText(newText)

                    // Put cursor back at same position (minus 1)
                    tvSecondaryBC.setSelection(cursorPosition - 1)

                    if (containsOperator(newText)) {
                        addedBC = false
                    }
                }
            }

            btEqualBC.setOnClickListener {
                vibratePhone(requireContext())
                handleEqualsPress()
            }
        }
    }
    private fun insertAtCursor(text: String) {
        vibratePhone(requireContext())

        if (addedBC && text in listOf("+", "-", "*", "/", "×", "÷")) {  // ✅ FIXED
            val result = binding.tvPrimaryBC.text.toString().replace(",", "")
            binding.tvSecondaryBC.setText(result)
            binding.tvSecondaryBC.append(text)
            binding.tvSecondaryBC.setSelection(binding.tvSecondaryBC.text.length)
            binding.tvPrimaryBC.text = ""
            binding.tvSecondaryBC.textSize = 45f  // Make input bigger
            binding.tvPrimaryBC.textSize = 35f     // Make result smaller
            addedBC = false
            isResultMode = false
            return
        }

        if (addedBC && text !in listOf("+", "-", "*", "/", "×", "÷", ".", "(", ")")) {  // ✅ FIXED
            binding.tvSecondaryBC.setText(text)
            binding.tvSecondaryBC.setSelection(text.length)
            binding.tvPrimaryBC.text = ""
            binding.tvSecondaryBC.textSize = 45f  // Make input bigger
            binding.tvPrimaryBC.textSize = 35f     // Make result smaller
            addedBC = false
            isResultMode = false
            return
        }

        val cursorPosition = binding.tvSecondaryBC.selectionStart
        val currentText = binding.tvSecondaryBC.text.toString()

        addToExpressionHistory(currentText)

        val newText = currentText.substring(0, cursorPosition) +
                text +
                currentText.substring(cursorPosition)

        binding.tvSecondaryBC.apply {
            setText(newText)
            setSelection(cursorPosition + text.length)
        }

        if (isResultMode) {
            binding.tvSecondaryBC.textSize = 45f  // Make input bigger
            binding.tvPrimaryBC.textSize = 35f     // Make result smaller
            isResultMode = false
        }
    }

    // Add this extension function at the bottom of the file (outside the class):
    fun TextView.animate(): android.view.ViewPropertyAnimator {
        return this.animate()
    }

    private fun android.view.ViewPropertyAnimator.textSize(size: Float): android.view.ViewPropertyAnimator {
        return this.withStartAction {
            (this@textSize as? TextView)?.let { textView ->
                android.animation.ValueAnimator.ofFloat(textView.textSize / textView.resources.displayMetrics.scaledDensity, size).apply {
                    duration = 200
                    addUpdateListener { animator ->
                        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, animator.animatedValue as Float)
                    }
                    start()
                }
            }
        }
    }

    private fun handlePercentage() {
        val currentText = binding.tvSecondaryBC.text.toString()

        if (currentText.isEmpty()) {
            showError("No value to convert to percentage", ErrorType.SYNTAX)
            return
        }

        try {
            val lastNumber = getLastNumber(currentText)

            if (lastNumber.isEmpty()) {
                showError("Invalid percentage operation", ErrorType.SYNTAX)
                return
            }

            val number = parseNumber(lastNumber) ?: run {
                showError("Invalid number for percentage", ErrorType.SYNTAX)
                return
            }

            val percentValue = number / 100.0
            val textBeforeNumber = currentText.dropLast(lastNumber.length)
            val formattedPercent = smartFormatResult(percentValue)

            addToExpressionHistory(currentText)
            binding.tvSecondaryBC.setText(textBeforeNumber + formattedPercent)

            AnalyticsManager.log("percentage_calculated", "value" to lastNumber)

        } catch (e: Exception) {
            showError("Percentage calculation error", ErrorType.CALCULATION)
        }
    }

    private fun setupScientificButtons() {
        binding.apply {
            btSecond.setOnClickListener {
                vibratePhone(requireContext())
                toggleSecondMode()
            }

            btDeg.setOnClickListener {
                vibratePhone(requireContext())
                toggleAngleMode()
            }

            btSin.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "asin" else "sin"
                onScientificFunctionClicked(function)
            }

            btCos.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "acos" else "cos"
                onScientificFunctionClicked(function)
            }

            btTan.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "atan" else "atan"
                onScientificFunctionClicked(function)
            }

            btRootX.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("sqrt")
            }

            btPowerXY.setOnClickListener {
                vibratePhone(requireContext())
                handlePowerOperation()
            }

            btLg.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("lg")
            }

            btLn.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("ln")
            }

            btFactorial.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("factorial")
            }

            btInverse.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("inverse")
            }

            btPi.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("pi")
            }
        }
    }

    private fun setupAdvancedButtons() {
        binding.apply {
            btSinh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("sinh")
            }

            btCosh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("cosh")
            }

            btTanh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("tanh")
            }

            btLog2.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("log2")
            }

            btE.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("e")
            }

            btScientificNotation.setOnClickListener {
                vibratePhone(requireContext())
                enableScientificNotation()
            }
        }
    }

    private fun setupMemoryButtons() {
        binding.apply {
            btMemoryAdd.setOnClickListener {
                vibratePhone(requireContext())
                val current = parseNumber(tvPrimaryBC.text.toString())
                if (current != null) {
                    memoryValue += current
                    showMemoryIndicator(memoryValue != 0.0)
                    saveMemoryState()
                    AnalyticsManager.log("memory_operation", "operation" to "add")
                }
            }

            btMemorySubtract.setOnClickListener {
                vibratePhone(requireContext())
                val current = parseNumber(tvPrimaryBC.text.toString())
                if (current != null) {
                    memoryValue -= current
                    showMemoryIndicator(memoryValue != 0.0)
                    saveMemoryState()
                    AnalyticsManager.log("memory_operation", "operation" to "subtract")
                }
            }

            btMemoryRecall.setOnClickListener {
                vibratePhone(requireContext())
                addToExpressionHistory(tvSecondaryBC.text.toString())
                tvSecondaryBC.setText(smartFormatResult(memoryValue))
                AnalyticsManager.log("memory_operation", "operation" to "recall")
            }

            btMemoryClear.setOnClickListener {
                vibratePhone(requireContext())
                memoryValue = 0.0
                showMemoryIndicator(false)
                saveMemoryState()
                AnalyticsManager.log("memory_operation", "operation" to "clear")
            }
        }
    }

    private fun setupControlButtons() {
        binding.apply {
            btUndo.setOnClickListener {
                vibratePhone(requireContext())
                val undoText = undo()
                if (undoText != null) {
                    tvSecondaryBC.setText(undoText)
                }
            }

            btRedo.setOnClickListener {
                vibratePhone(requireContext())
                val redoText = redo()
                if (redoText != null) {
                    tvSecondaryBC.setText(redoText)
                }
            }
        }
    }

    private fun handleEqualsPress() {
        if (isPowerMode && baseValue != null) {
            handlePowerCalculation()
        } else {
            handleRegularCalculation()
        }
    }

    private fun handlePowerCalculation() {
        val exponentInput = binding.tvSecondaryBC.text.toString().split("^").lastOrNull()?.toDoubleOrNull()

        if (exponentInput != null && baseValue != null) {
            val base = baseValue!!
            val result = base.pow(exponentInput)
            val historyExpression = "${base}^$exponentInput"

            isResultMode = true

            binding.tvSecondaryBC.apply {
                setText(historyExpression)
                setSelection(text.length)
            }

            binding.tvPrimaryBC.text = formatWithCommas(smartFormatResult(result))

            binding.tvSecondaryBC.textSize = 35f  // Make input smaller
            binding.tvPrimaryBC.textSize = 45f     // Make result bigger
            // Animate size swap
            switchToResultMode()

            isPowerMode = false
            baseValue = null

            HistoryManager.saveCalculation(requireContext(), historyExpression, smartFormatResult(result))
            addNewCalculationHistory(historyExpression, smartFormatResult(result))
            AnalyticsManager.logCalculationPerformed("Basic Calculator", "power")

            addedBC = true
        } else {
            showError("Invalid power operation", ErrorType.SYNTAX)
        }
    }


    private fun handleRegularCalculation() {
        try {
            val input = binding.tvSecondaryBC.text.toString()
            if (input.isNotEmpty()) {
                val result = safeEvaluate(input)

                when (result) {
                    is CalculationResult.Success -> {
                        val formattedResult = smartFormatResult(result.value)
                        addedBC = true
                        isResultMode = true
                        clearError()

                        binding.tvSecondaryBC.apply {
                            setText(input)
                            setSelection(text.length)
                        }

                        binding.tvPrimaryBC.text = formatWithCommas(formattedResult)

                        // Animate size swap
                        switchToResultMode()

                        binding.tvSecondaryBC.textSize = 35f  // Make input smaller
                        binding.tvPrimaryBC.textSize = 45f     // Make result bigger

                        HistoryManager.saveCalculation(requireContext(), input, formattedResult)
                        addNewCalculationHistory(input, formattedResult)
                        AnalyticsManager.logCalculationPerformed("Basic Calculator", "calculate")

                        AdFrequencyManager.trackBasicCalculatorCalculation(requireContext())
                        showInterstitialIfNeeded()
                    }
                    is CalculationResult.Error -> {
                        showError(result.message, ErrorType.CALCULATION)
                    }
                }
            }
        } catch (e: Exception) {
            showError("Calculation error", ErrorType.CALCULATION)
        }
    }

    private fun onScientificFunctionClicked(function: String) {
        val currentInput = parseNumber(binding.tvSecondaryBC.text.toString())

        if (currentInput == null && function !in listOf("pi", "e")) {
            showError("Invalid input for function", ErrorType.SYNTAX)
            return
        }

        val result: Double = try {
            calculateScientificFunction(function, currentInput)
        } catch (e: Exception) {
            showError("Math error: ${e.message}", ErrorType.DOMAIN)
            return
        }

        if (result.isInfinite() || result.isNaN()) {
            showError("Invalid result", ErrorType.OVERFLOW)
            return
        }

        addToExpressionHistory(binding.tvSecondaryBC.text.toString())

        val inputStr = currentInput?.toString() ?: ""
        val functionStr = getFunctionDisplayString(function, inputStr)

        binding.tvSecondaryBC.setText(functionStr)
        binding.tvPrimaryBC.text = formatWithCommas(smartFormatResult(result))

        AnalyticsManager.logCalculationPerformed("Basic Calculator", "scientific_$function")
    }

    private fun calculateScientificFunction(function: String, input: Double?): Double {
        return when (function) {
            "sin" -> if (isInDegreesMode) sin(Math.toRadians(input!!)) else sin(input!!)
            "cos" -> if (isInDegreesMode) cos(Math.toRadians(input!!)) else cos(input!!)
            "tan" -> if (isInDegreesMode) tan(Math.toRadians(input!!)) else tan(input!!)
            "asin" -> {
                validateInverseTrigInput(input!!)
                Math.toDegrees(asin(input))
            }
            "acos" -> {
                validateInverseTrigInput(input!!)
                Math.toDegrees(acos(input))
            }
            "atan" -> Math.toDegrees(atan(input!!))
            "sinh" -> sinh(input!!)
            "cosh" -> cosh(input!!)
            "tanh" -> tanh(input!!)
            "sqrt" -> {
                if (input!! < 0) throw ArithmeticException("Square root of negative number")
                sqrt(input)
            }
            "lg" -> {
                if (input!! <= 0) throw ArithmeticException("Logarithm of non-positive number")
                log10(input)
            }
            "ln" -> {
                if (input!! <= 0) throw ArithmeticException("Natural log of non-positive number")
                ln(input)
            }
            "log2" -> {
                if (input!! <= 0) throw ArithmeticException("Log base 2 of non-positive number")
                log2(input)
            }
            "factorial" -> calculateFactorial(input!!.toInt())
            "inverse" -> {
                if (input!! == 0.0) throw ArithmeticException("Division by zero")
                1 / input
            }
            "pi" -> if (input != null) input * PI else PI
            "e" -> if (input != null) input * E else E
            else -> input ?: 0.0
        }
    }

    private fun validateInverseTrigInput(input: Double) {
        if (input < -1 || input > 1) {
            throw ArithmeticException("Input out of domain for inverse trigonometric function")
        }
    }

    private fun calculateFactorial(n: Int): Double {
        if (n < 0) throw ArithmeticException("Factorial not defined for negative numbers")
        if (n > 170) throw ArithmeticException("Number too large for factorial")

        var result = 1.0
        for (i in 2..n) {
            result *= i
        }
        return result
    }

    private fun getFunctionDisplayString(function: String, input: String): String {
        return when (function) {
            "sin", "cos", "tan", "sinh", "cosh", "tanh", "asin", "acos", "atan" -> "$function($input)"
            "sqrt" -> "√($input)"
            "lg" -> "log($input)"
            "ln" -> "ln($input)"
            "log2" -> "log₂($input)"
            "factorial" -> "$input!"
            "inverse" -> "1/$input"
            "pi" -> if (input.isEmpty()) "π" else "$input×π"
            "e" -> if (input.isEmpty()) "e" else "$input×e"
            else -> function
        }
    }

    private fun handlePowerOperation() {
        val currentInput = parseNumber(binding.tvSecondaryBC.text.toString())

        if (currentInput != null) {
            baseValue = currentInput
            binding.tvSecondaryBC.setText("$currentInput^")
            isPowerMode = true
        } else {
            showError("Invalid input for power operation", ErrorType.SYNTAX)
        }
    }

    private fun toggleSecondMode() {
        isSecondMode = !isSecondMode

        binding.apply {
            if (isSecondMode) {
                btSin.text = "sin⁻¹"
                btCos.text = "cos⁻¹"
                btTan.text = "tan⁻¹"
                btDeg.isEnabled = false
                btDeg.alpha = 0.5f
            } else {
                btSin.text = "sin"
                btCos.text = "cos"
                btTan.text = "tan"
                btDeg.isEnabled = true
                btDeg.alpha = 1.0f
            }
        }

        AnalyticsManager.log("angle_mode_toggled", "mode" to if (isSecondMode) "inverse" else "normal")
    }

    private fun toggleAngleMode() {
        isInDegreesMode = !isInDegreesMode

        binding.btDeg.text = if (isInDegreesMode) "deg" else "rad"

        if (!isInDegreesMode) {
            disableSecondButton()
        } else {
            enableSecondButton()
        }

        AnalyticsManager.log("angle_unit_changed", "unit" to if (isInDegreesMode) "degrees" else "radians")
    }

    private fun enableScientificNotation() {
        val current = binding.tvSecondaryBC.text.toString()
        if (!current.contains("E") && current.isNotEmpty() && parseNumber(current) != null) {
            addToExpressionHistory(current)
            binding.tvSecondaryBC.setText("${current}E")
        }
    }

    private fun validateExpressionRealTime(expression: String): ValidationResult {
        return when {
            expression.isEmpty() -> ValidationResult.EMPTY
            hasConsecutiveOperators(expression) -> ValidationResult.CONSECUTIVE_OPERATORS
            hasUnbalancedParentheses(expression) -> ValidationResult.UNBALANCED_PARENTHESES
            endsWithOperator(expression) -> ValidationResult.INCOMPLETE
            hasInvalidCharacters(expression) -> ValidationResult.INVALID_CHARS
            else -> ValidationResult.VALID
        }
    }

    private fun hasConsecutiveOperators(expr: String): Boolean {
        return Regex("[+\\-*/×÷]{2,}").containsMatchIn(expr)
    }

    private fun hasUnbalancedParentheses(expr: String): Boolean {
        var count = 0
        for (char in expr) {
            when (char) {
                '(' -> count++
                ')' -> if (--count < 0) return true
            }
        }
        return count != 0
    }

    private fun endsWithOperator(expr: String): Boolean {
        return expr.isNotEmpty() && expr.last() in listOf('+', '-', '*', '/', '^', '×', '÷')
    }

    private fun hasInvalidCharacters(expr: String): Boolean {
        val validChars = "0123456789+-*/×÷().^E°π"
        return expr.any { it !in validChars }
    }

    private fun containsOperator(text: String): Boolean {
        return text.contains("+") || text.contains("-") ||
                text.contains("*") || text.contains("/") ||
                text.contains("×") || text.contains("÷")
    }

    private fun getLastNumber(expression: String): String {
        return expression.split(Regex("[+\\-*/×÷()]")).lastOrNull() ?: ""
    }

    private fun parseNumber(text: String): Double? {
        return try {
            when {
                text.isEmpty() -> null
                text.contains("π") -> text.replace("π", PI.toString()).toDoubleOrNull()
                text.contains("e") && !text.contains("E") -> text.replace("e", E.toString()).toDoubleOrNull()
                text.endsWith("°") -> text.dropLast(1).toDoubleOrNull()
                else -> text.toDoubleOrNull()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun safeEvaluate(expression: String): CalculationResult {
        return try {
            val sanitized = sanitizeExpression(expression)
            val validation = validateExpressionRealTime(sanitized)

            if (validation != ValidationResult.VALID) {
                return CalculationResult.Error(validation.message)
            }

            val result = CalculationUtil.evaluate(sanitized)

            when {
                result.isInfinite() -> CalculationResult.Error("Result too large")
                result.isNaN() -> CalculationResult.Error("Invalid operation")
                else -> CalculationResult.Success(result.toDouble())
            }
        } catch (e: ArithmeticException) {
            CalculationResult.Error("Math error: ${e.message}")
        } catch (e: Exception) {
            CalculationResult.Error("Calculation error")
        }
    }

    private fun sanitizeExpression(expression: String): String {
        return expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", PI.toString())
            .replace(" ", "")
            .replace("°", "")
    }

    private fun smartFormatResult(result: Double): String {
        return when {
            result.isInfinite() -> if (result > 0) "∞" else "-∞"
            result.isNaN() -> "Error"
            result == 0.0 -> "0"
            abs(result) < 1e-10 -> "0"
            abs(result) >= 1e9 -> String.format("%.6E", result)
            result == result.toInt().toDouble() -> result.toInt().toString()
            else -> {
                val formatted = String.format("%.12f", result).trimEnd('0').trimEnd('.')
                //if (formatted.length > 20) String.format("%.6E", result) else formatted
                formatted
            }
        }
    }

    private fun formatWithCommas(number: String): String {
        if (number.isEmpty() || number == "0" || number == "Error") return number

        // Handle special cases
        if (number == "∞" || number == "-∞") return number

        val parts = number.split(".")
        val intPart = parts[0].replace("-", "")
        val isNegative = number.startsWith("-")

        // Add commas to integer part
        val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()

        // Reconstruct with decimal if exists
        val result = if (parts.size > 1) "$formatted.${parts[1]}" else formatted
        return if (isNegative) "-$result" else result
    }

    private fun showMemoryIndicator(hasMemory: Boolean) {
        binding.memoryIndicator.visibility = if (hasMemory) View.VISIBLE else View.GONE
    }

    private fun saveMemoryState() {
        PrefUtil.setMemoryValue(requireContext(), memoryValue)
    }

    private fun restoreMemoryState() {
        memoryValue = PrefUtil.getMemoryValue(requireContext())
        showMemoryIndicator(memoryValue != 0.0)
    }

    private fun addToExpressionHistory(expression: String) {
        if (expression.isNotEmpty() && (expressionHistory.isEmpty() || expressionHistory.last() != expression)) {
            if (historyIndex < expressionHistory.size - 1) {
                expressionHistory.subList(historyIndex + 1, expressionHistory.size).clear()
            }

            expressionHistory.add(expression)
            historyIndex = expressionHistory.size - 1

            if (expressionHistory.size > 50) {
                expressionHistory.removeAt(0)
                historyIndex--
            }
        }
    }

    private fun undo(): String? {
        return if (historyIndex > 0) {
            historyIndex--
            expressionHistory[historyIndex]
        } else null
    }

    private fun redo(): String? {
        return if (historyIndex < expressionHistory.size - 1) {
            historyIndex++
            expressionHistory[historyIndex]
        } else null
    }

    private fun showError(message: String, type: ErrorType = ErrorType.CALCULATION) {
        binding.tvPrimaryBC.apply {
            text = when (type) {
                ErrorType.CALCULATION -> "Error"
                ErrorType.SYNTAX -> "Syntax Error"
                ErrorType.OVERFLOW -> "Overflow"
                ErrorType.DOMAIN -> "Math Error"
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color))
        }

        binding.tvErrorBC.apply {
            text = message
            visibility = View.VISIBLE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            clearError()
        }, 3000)
    }

    private fun clearError() {
        binding.tvErrorBC.visibility = View.GONE
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
        binding.tvPrimaryBC.setTextColor(typedValue.data)
    }

    private fun disableSecondButton() {
        binding.btSecond.isEnabled = false
        binding.btSecond.alpha = 0.5f
    }

    private fun enableSecondButton() {
        binding.btSecond.isEnabled = true
        binding.btSecond.alpha = 1.0f
    }

    private fun resetCalculatorState() {
        isPowerMode = false
        baseValue = null
        isSecondMode = false
    }

    private fun toggleBarLogic() {
        binding.toggleBar.setOnClickListener {
            if (isPanelVisible) {
                slideDown(binding.scientificButtonsPanel)
                slideDown(binding.advancedScientificPanel)
                slideDown(binding.memoryControlPanel)
                binding.toggleArrow.rotation = 0f
            } else {
                slideUp(binding.scientificButtonsPanel)
                slideUp(binding.advancedScientificPanel)
                slideUp(binding.memoryControlPanel)
                binding.toggleArrow.rotation = 180f

                AnalyticsManager.log("scientific_panel_opened")
            }
            isPanelVisible = !isPanelVisible
        }
    }

    private fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun slideDown(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat())
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }
            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            val secondaryText = PrefUtil.getSecondaryTextBC(requireContext()) ?: ""

            if (secondaryText.isNotEmpty() && validateExpressionRealTime(secondaryText) == ValidationResult.VALID) {
                tvSecondaryBC.setText(secondaryText)
            }
        }
        restoreMemoryState()
    }

    override fun onStop() {
        super.onStop()
        binding.apply {
            PrefUtil.setSecondaryTextBC(requireContext(), tvSecondaryBC.text.toString())
        }
        saveMemoryState()
    }

    override fun onDestroy() {
        super.onDestroy()
        inputRunnable?.let { inputHandler.removeCallbacks(it) }
    }
}