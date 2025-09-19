package com.aatmik.calculator.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.aatmik.calculator.util.CalculatorCategoriesUtil
import com.aatmik.calculator.util.CalculatorUtils
import com.aatmik.calculator.util.NetworkUtil
import com.aatmik.calculator.util.ThemeManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // recycler view for calculators
    lateinit var calculatorRV: RecyclerView
    lateinit var calculatorAdapter: CalculatorAdapter
    lateinit var calculatorList: ArrayList<Calculator>
    private lateinit var originalCalculatorList: ArrayList<Calculator>

    // recycler view for categories
    lateinit var categoriesRV: RecyclerView
    lateinit var categoryAdapter: CategoryAdapter
    private var currentSelectedCategory = "All"

    companion object {
        private const val GRID_COLUMN_COUNT = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.initializeTheme(this)
        setTheme(ThemeManager.getThemeStyle(this))
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for Android 15 compatibility
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system bar insets for edge-to-edge
        setupEdgeToEdgeInsets()

        runAds()
        loadCalculatorOrder()
        setupCategoriesRecyclerView()
        setupRecyclerView()
        search()
        binding.menuIv.setOnClickListener {
            showBottomSheet()
        }
        binding.calculatorCv.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            intent?.let { startActivity(it) }
        }
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
            filterByCategory(selectedCategory)
        }

        categoriesRV.adapter = categoryAdapter
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

        bottomSheetBinding.rateApp.setOnClickListener {
            rateApp()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnRemoveAds.setOnClickListener {
            removeAds()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnGetUpdate.setOnClickListener {
            updateApp()
            bottomSheetDialog.dismiss()
        }
        bottomSheetBinding.btnTheme.setOnClickListener {
            bottomSheetDialog.dismiss()
            showThemeSelector()
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
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

                ThemeManager.saveTheme(this, selectedTheme)
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
    }

    private fun removeAds() {
        Toast.makeText(this, "Removing ads / Starting premium subscription", Toast.LENGTH_SHORT)
            .show()
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
        // Just load the default calculator list
        calculatorList = CalculatorUtils.calculatorList
        originalCalculatorList = ArrayList(CalculatorUtils.calculatorList)
    }

    private fun search() {
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

            "Stopwatch" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Time Zone Converter" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Percentage" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Interest Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Loan Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Fuel Economy Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Ohm's Law" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Trip Estimate" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Tax Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Contribution Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
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

            "Love Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Free Fall" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Area Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "GPA Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Age" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Length" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Weight" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Speed" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Tip" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Body Mass Index" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Calorie Calculator" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Shapes" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Equation" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Currency Converter" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Temperature" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Bodies" -> Intent(this, CalculatorActivity::class.java).apply {
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