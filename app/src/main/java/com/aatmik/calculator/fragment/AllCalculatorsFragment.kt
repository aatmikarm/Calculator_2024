package com.aatmik.calculator.fragment

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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.activity.CalculatorActivity
import com.aatmik.calculator.activity.ContainerActivity
import com.aatmik.calculator.adapter.CalculatorAdapter
import com.aatmik.calculator.adapter.CategoryAdapter
import com.aatmik.calculator.databinding.FragmentAllCalculatorsBinding
import com.aatmik.calculator.databinding.BottomSheetLayoutBinding
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.CalculatorCategoriesUtil
import com.aatmik.calculator.util.CalculatorUtils
import com.aatmik.calculator.util.SubscriptionManager
import com.aatmik.calculator.util.ThemeManager
import com.aatmik.calculator.util.UpdateManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class AllCalculatorsFragment : Fragment() {

    private var _binding: FragmentAllCalculatorsBinding? = null
    private val binding get() = _binding!!

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

    // Collapsible category section
    private var isCategoryExpanded = false

    // Gesture detector for swipe functionality
    private lateinit var gestureDetector: GestureDetectorCompat

    companion object {
        private const val CALCULATOR_GRID_COLUMN_COUNT = 4
        private const val CATEGORY_GRID_COLUMN_COUNT = 3
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllCalculatorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("AllCalculatorsFragment", "onViewCreated started")

        // Check for app updates automatically on startup
        UpdateManager.checkForUpdatesAutomatically(requireActivity())

        Log.d("AllCalculatorsFragment", "About to load calculator order")
        loadCalculatorOrder()
        Log.d("AllCalculatorsFragment", "Calculator list size: ${calculatorList.size}")

        setupGestureDetector()
        setupCategoriesRecyclerView()
        setupCategoryCollapsible()

        Log.d("AllCalculatorsFragment", "About to setup RecyclerView")
        setupRecyclerView()
        filterByCategory("All")
        Log.d("AllCalculatorsFragment", "RecyclerView setup completed")

        search()

        binding.menuIv.setOnClickListener {
            showBottomSheet()
        }

        binding.calculatorCv.setOnClickListener {
            // Navigate to Basic Calculator page using ViewPager
            (requireActivity() as? ContainerActivity)?.navigateToPage(
                ContainerActivity.PAGE_BASIC_CALCULATOR,
                true
            )
        }

        Log.d("AllCalculatorsFragment", "onViewCreated completed")
    }

    /**
     * Setup collapsible category section
     */
    private fun setupCategoryCollapsible() {
        binding.categoryHeader.setOnClickListener {
            toggleCategoryExpansion()
        }
    }

    /**
     * Toggle category section expansion with animation
     */
    private fun toggleCategoryExpansion() {
        if (isCategoryExpanded) {
            // Collapse
            collapseView(binding.categoryExpandableContent)
            rotateIcon(binding.categoryExpandIcon, 180f, 0f)
            isCategoryExpanded = false
        } else {
            // Expand
            expandView(binding.categoryExpandableContent)
            rotateIcon(binding.categoryExpandIcon, 0f, 180f)
            isCategoryExpanded = true
        }
    }

    /**
     * Expand view with animation
     */
    private fun expandView(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * Collapse view with animation
     */
    private fun collapseView(view: View) {
        view.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                view.visibility = View.GONE
            }
            .start()
    }

    /**
     * Rotate icon animation
     */
    private fun rotateIcon(view: View, fromDegree: Float, toDegree: Float) {
        view.animate()
            .rotation(toDegree)
            .setDuration(300)
            .start()
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

        gestureDetector = GestureDetectorCompat(requireContext(), gestureListener)
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

    private fun setupCategoriesRecyclerView() {
        categoriesRV = binding.categoriesRV
        // Use GridLayoutManager with 3 columns for categories
        categoriesRV.layoutManager = GridLayoutManager(requireContext(), CATEGORY_GRID_COLUMN_COUNT)

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

        // Collapse category section after selection
        if (isCategoryExpanded) {
            toggleCategoryExpansion()
        }

        // Auto-scroll to calculator section
//        binding.scrollView.post {
//            // Calculate the position to scroll to (categories height + some offset)
//            val calculatorSectionY = binding.linearLayout.top
//            binding.scrollView.smoothScrollTo(0, calculatorSectionY)
//        }
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
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
                requireContext(),
                "No email app found. Please send an email to: $supportEmail",
                Toast.LENGTH_LONG
            ).show()

            // Copy email to clipboard as fallback
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
                requireActivity().recreate() // Restart activity to apply new theme
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
            Toast.makeText(requireContext(), "You're already a Premium user! 🎉", Toast.LENGTH_SHORT).show()
            AnalyticsManager.log("premium_already_active")
            return
        }

        // Show premium dialog
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

    private fun loadCalculatorOrder() {
        val sharedPreferences = requireContext().getSharedPreferences("CalculatorPrefs", Context.MODE_PRIVATE)
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
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
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
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        calculatorRV.layoutManager = GridLayoutManager(requireContext(), CALCULATOR_GRID_COLUMN_COUNT)

        calculatorAdapter = CalculatorAdapter(calculatorList) { calculator ->
            handleCalculatorSelection(calculator.name)
        }

        calculatorRV.adapter = calculatorAdapter
    }

    private fun handleCalculatorSelection(calculatorName: String) {
        val intent = when (calculatorName) {
            "Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Convertor" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Currency Converter" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Percentage" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Age" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Tip" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Body Mass Index" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "FD Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Loan Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Interest Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "GST Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Temperature" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Length" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Weight" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Area Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Calorie Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Investment Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Tax Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Speed" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Fuel Economy Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "GPA Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Budget Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Stopwatch" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Fraction Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ratio Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "ROI Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Break-Even Analysis" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "NPV & IRR Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Loan Comparison" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Shapes" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Equation" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Time Zone Converter" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Proportion Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Trip Estimate" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "LCM & GCD Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Number Tables" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Pregnancy Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Period Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ovulation & Fertility" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Punnett Square" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Cell Dilution" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Contribution Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ohm's Law" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Beam Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Column Buckling" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Voltage Divider" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Function Grapher" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Statistical Graph Generator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Coordinate Geometry" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Free Fall" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Bodies" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Lightning Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Compass" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Level" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Molarity Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "pH Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Stoichiometry Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Ideal Gas Law" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Love Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Friendship Calculator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Roll a Dice" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            "Random Number Generator" -> Intent(requireContext(), CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }
            else -> null
        }

        intent?.let { startActivity(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}