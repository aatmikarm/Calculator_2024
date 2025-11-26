package com.aatmik.calculator.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.aatmik.calculator.adapter.CalculatorPagerAdapter
import com.aatmik.calculator.databinding.ActivityContainerBinding
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ThemeManager

class ContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContainerBinding
    private lateinit var pagerAdapter: CalculatorPagerAdapter

    companion object {
        const val PAGE_BASIC_CALCULATOR = 0
        const val PAGE_ALL_CALCULATORS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.initializeTheme(this)
        setTheme(ThemeManager.getThemeStyle(this))
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdgeInsets()
        setupViewPager()
        setupBackPress()

        AnalyticsManager.log("container_activity_opened")
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                insets.top,
                view.paddingRight,
                insets.bottom
            )
            windowInsets
        }
    }

    private fun setupViewPager() {
        pagerAdapter = CalculatorPagerAdapter(this)

        binding.viewPager.apply {
            adapter = pagerAdapter

            // Start with Basic Calculator (page 0)
            setCurrentItem(PAGE_BASIC_CALCULATOR, false)

            // Keep both pages in memory for smooth transitions
            offscreenPageLimit = 1

            // Register page change callback
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        PAGE_BASIC_CALCULATOR -> {
                            AnalyticsManager.log("page_changed", "screen" to "basic_calculator")
                        }
                        PAGE_ALL_CALCULATORS -> {
                            AnalyticsManager.log("page_changed", "screen" to "all_calculators")
                        }
                    }
                }
            })
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (binding.viewPager.currentItem) {
                    PAGE_ALL_CALCULATORS -> {
                        // If on All Calculators page, go back to Basic Calculator
                        binding.viewPager.setCurrentItem(PAGE_BASIC_CALCULATOR, true)
                    }
                    PAGE_BASIC_CALCULATOR -> {
                        // If on Basic Calculator, exit app
                        finish()
                    }
                }
            }
        })
    }

    /**
     * Public method to switch pages programmatically
     */
    fun navigateToPage(page: Int, smooth: Boolean = true) {
        binding.viewPager.setCurrentItem(page, smooth)
    }
}