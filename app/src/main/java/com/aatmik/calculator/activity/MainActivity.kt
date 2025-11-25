package com.aatmik.calculator.activity

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowMetrics
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.adapter.CalculatorAdapter
import com.aatmik.calculator.adapter.CategoryAdapter
import com.aatmik.calculator.databinding.ActivityMainBinding
import com.aatmik.calculator.databinding.BottomSheetLayoutBinding
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.model.Category
import com.aatmik.calculator.util.AdConfig
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.CalculatorCategoriesUtil
import com.aatmik.calculator.util.CalculatorUtils
import com.aatmik.calculator.util.NetworkUtil
import com.aatmik.calculator.util.SubscriptionManager
import com.aatmik.calculator.util.ThemeManager
import com.aatmik.calculator.util.UpdateManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var analytics: FirebaseAnalytics
    private val TAG = "MainActivity"

    // Session tracking
    private var sessionStartTime: Long = 0

    // recycler view for calculators
    lateinit var calculatorRV: RecyclerView
    lateinit var calculatorAdapter: CalculatorAdapter
    lateinit var calculatorList: ArrayList<Calculator>
    private lateinit var originalCalculatorList: ArrayList<Calculator>

    // recycler view for categories
    lateinit var categoriesRV: RecyclerView
    lateinit var categoryAdapter: CategoryAdapter
    private var currentSelectedCategory = "All"
    private var currentCategoryIndex = 0

    // Gesture detector for swipe functionality
    private lateinit var gestureDetector: GestureDetectorCompat

    companion object {
        private const val GRID_COLUMN_COUNT = 4
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onCreate(savedInstanceState: Bundle?)  {
        ThemeManager.initializeTheme(this)
        setTheme(ThemeManager.getThemeStyle(this))
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate started")

        // Enable edge-to-edge for Android 15 compatibility
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Safe Firebase initialization
//        try {
//            // Initialize Firebase if not already initialized
//            if (FirebaseApp.getApps(this).isEmpty()) {
//                FirebaseApp.initializeApp(this)
//            }
//
//            // Initialize Analytics Manager
//            AnalyticsManager.init(this)
//            AnalyticsManager.logAppOpen()
//            Log.d("MainActivity", "Firebase Analytics initialized successfully")
//        } catch (e: Exception) {
//            Log.e("MainActivity", "Firebase initialization failed: ${e.message}")
//        }

        // Handle system bar insets for edge-to-edge
        setupEdgeToEdgeInsets()

        // Check for app updates automatically on startup
        UpdateManager.checkForUpdatesAutomatically(this)

        runAds()

        Log.d("MainActivity", "About to load calculator order")
        loadCalculatorOrder()
        Log.d("MainActivity", "Calculator list size: ${calculatorList.size}")

        setupGestureDetector()
        setupCategoriesRecyclerView()

        Log.d("MainActivity", "About to setup RecyclerView")
        setupRecyclerView()
        filterByCategory("All")
        Log.d("MainActivity", "RecyclerView setup completed")

        search()
        binding.menuIv.setOnClickListener {
            showBottomSheet()
        }
        binding.calculatorCv.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            intent?.let { startActivity(it) }
        }

        Log.d("MainActivity", "onCreate completed")
    }

    /**
     * Setup gesture detector for swipe functionality
     */
    private fun setupGestureDetector() {
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // Check if it's a horizontal swipe
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous category
                            onSwipeRight()
                        } else {
                            // Swipe left - go to next category
                            onSwipeLeft()
                        }
                        return true
                    }
                }
                return false
            }
        }

        gestureDetector = GestureDetectorCompat(this, gestureListener)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Only detect swipes in the calculator grid area
        val calculatorGridTop = binding.linearLayout.top
        val calculatorGridBottom = binding.linearLayout.bottom

        // Check if touch is within the calculator grid bounds
        if (ev.y >= calculatorGridTop && ev.y <= calculatorGridBottom) {
            gestureDetector.onTouchEvent(ev)
        }

        return super.dispatchTouchEvent(ev)
    }

    /**
     * Handle swipe left gesture - move to next category
     */
    private fun onSwipeLeft() {
        val totalCategories = CalculatorCategoriesUtil.categories.size
        if (currentCategoryIndex < totalCategories - 1) {
            currentCategoryIndex++
            selectCategoryByIndex(currentCategoryIndex)
            AnalyticsManager.log("category_swiped", "direction" to "left")
        }
    }

    /**
     * Handle swipe right gesture - move to previous category
     */
    private fun onSwipeRight() {
        if (currentCategoryIndex > 0) {
            currentCategoryIndex--
            selectCategoryByIndex(currentCategoryIndex)
            AnalyticsManager.log("category_swiped", "direction" to "right")
        }
    }

    /**
     * Select category by index and update UI
     */
    private fun selectCategoryByIndex(index: Int) {
        val selectedCategory = CalculatorCategoriesUtil.categories[index].name
        currentSelectedCategory = selectedCategory
        currentCategoryIndex = index

        // Update category adapter selection
        categoryAdapter.updateSelection(index)

        // Scroll to make selected category visible
        categoriesRV.scrollToPosition(index)

        // Filter calculators by category
        filterByCategory(selectedCategory)
    }

    /**
     * Handle window insets for proper edge-to-edge layout
     */
    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply top padding for status bar
            view.setPadding(
                view.paddingLeft,
                insets.top,
                view.paddingRight,
                insets.bottom
            )

            windowInsets
        }
    }

    private fun setupCategoriesRecyclerView() {
        categoriesRV = binding.categoriesRV
        categoriesRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        categoryAdapter = CategoryAdapter(CalculatorCategoriesUtil.categories) { position ->
            val selectedCategory = CalculatorCategoriesUtil.categories[position].name
            currentSelectedCategory = selectedCategory
            currentCategoryIndex = position
            filterByCategory(selectedCategory)

            AnalyticsManager.log("category_selected", "category" to selectedCategory)
        }

        categoriesRV.adapter = categoryAdapter

        // Ensure proper initial state
        currentSelectedCategory = "All"
        currentCategoryIndex = 0

        // Scroll to the "All" category at the beginning
        categoriesRV.scrollToPosition(0)
    }

    private fun filterByCategory(categoryName: String) {
        val searchQuery = binding.searchEt.text.toString()

        val filteredList = if (searchQuery.isEmpty()) {
            CalculatorCategoriesUtil.getCalculatorsForCategory(categoryName, originalCalculatorList)
        } else {
            CalculatorCategoriesUtil.filterCalculatorsBySearch(searchQuery, categoryName, originalCalculatorList)
        }

        calculatorAdapter.updateCalculatorList(filteredList)
    }

    private fun runAds() {
        if (NetworkUtil.isNetworkAvailable(this)) {
            loadBanner()
        } else {
            Log.d("NetworkCheck", "No internet connection available.")
        }
    }

    private var adView: AdView? = null

    // Get the ad size with screen width.
    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun loadBanner() {
        // Check if ads are enabled (will be false if user is premium)
        if (!AdConfig.areAdsEnabled()) {
            Log.d("BannerAd", "User is premium - No ads!")
            binding.adViewContainer.visibility = View.GONE
            return
        }

        if (AdConfig.getBannerAdId().isEmpty()) {
            binding.adViewContainer.visibility = View.GONE
            return
        }

        val adView = AdView(this)
        adView.adUnitId = AdConfig.getBannerAdId()
        adView.setAdSize(adSize)
        this.adView = adView

        binding.adViewContainer.removeAllViews()
        binding.adViewContainer.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)

        // Make theme button visible
        bottomSheetBinding.btnTheme.visibility = View.VISIBLE

        // Show premium status on text
        if (SubscriptionManager.isPremium()) {
            // Change text to show user is already premium
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
            // Check for updates manually when user clicks update button
            UpdateManager.checkForUpdatesManually(this)
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
            Toast.makeText(this, "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCustomerSupport() {
        val supportEmail = "aatmikarm@gmail.com"
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
                this,
                "No email app found. Please send an email to: $supportEmail",
                Toast.LENGTH_LONG
            ).show()

            // Copy email to clipboard as fallback
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Support Email", supportEmail)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Email address copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
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

        val currentTheme = ThemeManager.getSavedTheme(this)
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

        androidx.appcompat.app.AlertDialog.Builder(this)
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
                ThemeManager.saveTheme(this, selectedTheme)
                AnalyticsManager.logThemeChanged(themeName)
                recreate() // Restart activity to apply new theme
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            Toast.makeText(this, "You're already a Premium user! 🎉", Toast.LENGTH_SHORT).show()
            AnalyticsManager.log("premium_already_active")
            return
        }

        // Show premium dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Go Premium")
            .setMessage("Remove all ads and unlock all features for just ₹200/year!\n\n✓ No Banner Ads\n✓ No Interstitial Ads\n✓ All Features Unlocked\n✓ Works on all your devices")
            .setPositiveButton("Subscribe ₹200/year") { _, _ ->
                SubscriptionManager.startSubscriptionPurchase(this) { error ->
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        AnalyticsManager.log("remove_ads_clicked")
    }

    private fun updateApp() {
        val appPackageName = "com.aatmik.calculator"
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
            )
        )
    }

    private fun loadCalculatorOrder() {
        val sharedPreferences = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE)
        val savedOrder = sharedPreferences.getString("CalculatorOrder", null)

        if (!savedOrder.isNullOrEmpty()) {
            val orderedNames = savedOrder.split(",")
            val orderedList = arrayListOf<Calculator>()

            // Rebuild the calculator list based on saved order
            for (name in orderedNames) {
                val calculator = CalculatorUtils.calculatorList.find { it.name == name }
                calculator?.let {
                    orderedList.add(it)
                }
            }
            calculatorList = orderedList
        } else {
            // Load default list if no saved order exists
            calculatorList = ArrayList(CalculatorUtils.calculatorList)
        }

        // Initialize originalCalculatorList
        originalCalculatorList = ArrayList(CalculatorUtils.calculatorList)
    }

    private fun search() {
        var searchJob: Job? = null

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val searchQuery = s.toString()
                filterBySearch(searchQuery)

                // Show the clear (cross) button if there's text
                if (!s.isNullOrEmpty()) {
                    binding.clearTextIv.visibility = View.VISIBLE
                } else {
                    binding.clearTextIv.visibility = View.GONE
                }
                // Restore cursor visibility when the user starts typing again
                if (s.isNullOrEmpty().not()) {
                    binding.searchEt.isCursorVisible = true
                }

                // Debounce search analytics - only log after user stops typing for 1 second
                searchJob?.cancel()
                if (!s.isNullOrEmpty() && s.length >= 3) {
                    searchJob = lifecycleScope.launch {
                        delay(1000) // 1 second delay
                        AnalyticsManager.logSearch(searchQuery)
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                val searchQuery = p0.toString()
                filterBySearch(searchQuery)
            }
        })

        // Set up click listener for clear (cross) button
        binding.clearTextIv.setOnClickListener {
            // Clear the text
            binding.searchEt.text.clear()
            // Hide the clear button
            binding.clearTextIv.visibility = View.GONE
            // Hide the keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEt.windowToken, 0)
            // Hide the cursor when text is cleared
            binding.searchEt.isCursorVisible = false

            AnalyticsManager.log("search_cleared")
        }
    }

    private fun filterBySearch(searchQuery: String) {
        val filteredList = CalculatorCategoriesUtil.filterCalculatorsBySearch(
            searchQuery,
            currentSelectedCategory,
            originalCalculatorList
        )
        calculatorAdapter.updateCalculatorList(filteredList)
    }

    private fun setupRecyclerView() {
        calculatorRV = binding.calculatorRV
        calculatorRV.layoutManager = GridLayoutManager(this, GRID_COLUMN_COUNT)

        calculatorAdapter = CalculatorAdapter(calculatorList) { calculator ->
            handleCalculatorSelection(calculator.name)
        }

        calculatorRV.adapter = calculatorAdapter
    }

    private fun handleCalculatorSelection(calculatorName: String) {
        val intent = when (calculatorName) {
            "Basic" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Convertor" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Currency Converter" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Percentage" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Age" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Tip" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Body Mass Index" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "FD Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Loan Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Interest Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "GST Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Temperature" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Length" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Weight" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Area Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Calorie Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Investment Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Tax Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Speed" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Fuel Economy Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "GPA Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Budget Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Stopwatch" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Fraction Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ratio Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "ROI Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Break-Even Analysis" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "NPV & IRR Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Loan Comparison" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Shapes" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Equation" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Time Zone Converter" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Proportion Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Trip Estimate" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "LCM & GCD Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Number Tables" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Pregnancy Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Period Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ovulation & Fertility" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Punnett Square" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Cell Dilution" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Contribution Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ohm's Law" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Beam Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Column Buckling" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Voltage Divider" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Function Grapher" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Statistical Graph Generator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Coordinate Geometry" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Free Fall" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Bodies" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Lightning Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Compass" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Level" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Molarity Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "pH Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Stoichiometry Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ideal Gas Law" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Love Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Friendship Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Roll a Dice" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Random Number Generator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            else -> null
        }

        intent?.let { startActivity(it) }
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}