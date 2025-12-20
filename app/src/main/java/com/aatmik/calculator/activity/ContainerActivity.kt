package com.aatmik.calculator.activity

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.aatmik.calculator.adapter.CalculatorPagerAdapter
import com.aatmik.calculator.databinding.ActivityContainerBinding
import com.aatmik.calculator.util.AdConfig
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.FirebaseConfigManager
import com.aatmik.calculator.util.NetworkUtil
import com.aatmik.calculator.util.ThemeManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class ContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContainerBinding
    private lateinit var pagerAdapter: CalculatorPagerAdapter
    private var adView: AdView? = null
    private var adRetryCount = 0
    private val maxAdRetries = 3
    private var isAdLoading = false

    companion object {
        private const val TAG = "ContainerActivity"
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

        // Load banner ad once in parent activity
        loadBannerAd()

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

    private fun loadBannerAd() {
        Log.d(TAG, "Starting banner ad load (attempt ${adRetryCount + 1}/$maxAdRetries)")

        // Prevent multiple simultaneous loads
        if (isAdLoading) {
            Log.d(TAG, "Ad already loading, skipping")
            return
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Log.d(TAG, "No internet connection available")
            binding.adViewContainer.visibility = View.GONE

            // Retry after network might be available
            scheduleAdRetry(30000) // Retry after 30 seconds for network issues
            return
        }

        if (!AdConfig.areAdsEnabled()) {
            Log.d(TAG, "Ads disabled (premium user)")
            binding.adViewContainer.visibility = View.GONE
            return
        }

        // Check Remote Config
        if (!FirebaseConfigManager.shouldShowBannerAds()) {
            Log.d("BannerAd", "Banner ads disabled via Remote Config")
            binding.adViewContainer.visibility = View.GONE
            return
        }

        if (AdConfig.getBannerAdId().isEmpty()) {
            Log.d(TAG, "Banner ad ID is empty")
            binding.adViewContainer.visibility = View.GONE
            return
        }

        Log.d(TAG, "Loading ad with ID: ${AdConfig.getBannerAdId()}")
        Log.d(TAG, "Test mode: ${AdConfig.isUsingTestAds()}")

        isAdLoading = true

        // Create AdView only once or reuse existing
        if (adView == null) {
            val newAdView = AdView(this)
            newAdView.adUnitId = AdConfig.getBannerAdId()
            newAdView.setAdSize(getAdSize())
            this.adView = newAdView

            binding.adViewContainer.removeAllViews()
            binding.adViewContainer.addView(newAdView)
        }

        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d(TAG, "Banner ad loaded successfully")
                binding.adViewContainer.visibility = View.VISIBLE
                isAdLoading = false
                adRetryCount = 0 // Reset retry count on success
                AnalyticsManager.log("banner_ad_loaded",
                    "location" to "container",
                    "attempt" to "${adRetryCount + 1}")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                Log.e(TAG, "Failed: ${loadAdError.message} (code: ${loadAdError.code})")
                isAdLoading = false

                AnalyticsManager.log("banner_ad_failed",
                    "error" to loadAdError.message,
                    "code" to loadAdError.code.toString(),
                    "attempt" to "${adRetryCount + 1}")

                // Smart retry logic based on error code
                when (loadAdError.code) {
                    AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                        // Network issue - retry with longer delay
                        scheduleAdRetry(15000) // 15 seconds
                    }
                    AdRequest.ERROR_CODE_NO_FILL -> {
                        // No ad inventory - retry with longer delay
                        scheduleAdRetry(60000) // 60 seconds - no point retrying immediately
                    }
                    AdRequest.ERROR_CODE_INTERNAL_ERROR -> {
                        // Internal error - retry with medium delay
                        scheduleAdRetry(10000) // 10 seconds
                    }
                    else -> {
                        // Other errors - standard retry
                        scheduleAdRetry(8000) // 8 seconds
                    }
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                Log.d(TAG, "Banner ad clicked")
                AnalyticsManager.log("banner_ad_clicked")
            }

            override fun onAdOpened() {
                super.onAdOpened()
                Log.d(TAG, "Banner ad opened")
            }

            override fun onAdClosed() {
                super.onAdClosed()
                Log.d(TAG, "Banner ad closed")
            }
        }

        // Show container while loading to reserve space (prevents layout jump)
        binding.adViewContainer.visibility = View.VISIBLE

        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)
    }

    private fun scheduleAdRetry(delayMillis: Long) {
        if (adRetryCount >= maxAdRetries) {
            Log.d(TAG, "Max retries reached ($maxAdRetries), giving up")
            binding.adViewContainer.visibility = View.GONE
            return
        }

        adRetryCount++
        Log.d(TAG, "Scheduling retry in ${delayMillis}ms (attempt ${adRetryCount + 1}/$maxAdRetries)")

        binding.root.postDelayed({
            if (!isDestroyed && !isFinishing) {
                loadBannerAd()
            }
        }, delayMillis)
    }

    /**
     * Get adaptive banner ad size based on screen width
     */
    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
                windowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    /**
     * Public method to switch pages programmatically
     */
    fun navigateToPage(page: Int, smooth: Boolean = true) {
        binding.viewPager.setCurrentItem(page, smooth)
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()

        // If ad failed to load and we haven't exceeded retries, try again on resume
        if (adView != null && binding.adViewContainer.visibility == View.GONE && adRetryCount < maxAdRetries) {
            Log.d(TAG, "Activity resumed, retrying ad load")
            binding.root.postDelayed({
                if (!isDestroyed && !isFinishing) {
                    loadBannerAd()
                }
            }, 2000) // Small delay to let activity fully resume
        }
    }

    override fun onPause() {
        super.onPause()
        adView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView?.destroy()
        adView = null
    }
}