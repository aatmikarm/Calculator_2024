package com.aatmik.calculator.activity

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.ActivityCalculatorBinding
import com.aatmik.calculator.fragment.AgeFragment
import com.aatmik.calculator.fragment.BasicCalculatorFragment
import com.aatmik.calculator.fragment.BodyMassIndexFragment
import com.aatmik.calculator.fragment.ConverterFragment
import com.aatmik.calculator.fragment.CurrencyConverterFragment
import com.aatmik.calculator.fragment.LengthFragment
import com.aatmik.calculator.fragment.PercentageFragment
import com.aatmik.calculator.fragment.SpeedFragment
import com.aatmik.calculator.fragment.StopwatchFragment
import com.aatmik.calculator.fragment.TemperatureFragment
import com.aatmik.calculator.fragment.TipFragment
import com.aatmik.calculator.fragment.WeightFragment
import com.aatmik.calculator.fragment.bodies.BodiesFragment
import com.aatmik.calculator.fragment.shapes.ShapesFragment
import com.aatmik.calculator.util.AdConfig
import com.aatmik.calculator.util.NetworkUtil
import com.example.yourapp.MathEquationSolverFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalculatorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalculatorBinding
    private lateinit var adRequest: AdRequest
    private lateinit var interstitialAd1: InterstitialAd
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        runAds()
        // Get the calculator type passed from MainActivity
        val calculatorType = intent.getStringExtra("calculatorName")

        if (calculatorType == "Stopwatch" || calculatorType == "Convertor") {
            normalLaunch(calculatorType)
        } else {
            coroutineLaunch(calculatorType)
        }


    }

    private fun coroutineLaunch(calculatorType: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            setUpCalculator(calculatorType)
        }
    }

    private fun normalLaunch(calculatorType: String) {
        setUpCalculator(calculatorType)
    }


    private fun setUpCalculator(calculatorType: String?) {
        if (supportFragmentManager.findFragmentById(R.id.calculatorFragmentContainer) == null) {
            when (calculatorType) {
                "Basic" -> loadFragment(BasicCalculatorFragment())
                "Convertor" -> loadFragment(ConverterFragment())
                "Stopwatch" -> loadFragment(StopwatchFragment())
                "Percentage" -> loadFragment(PercentageFragment())
                "Age" -> loadFragment(AgeFragment())
                "Length" -> loadFragment(LengthFragment())
                "Weight" -> loadFragment(WeightFragment())
                "Speed" -> loadFragment(SpeedFragment())
                "Tip" -> loadFragment(TipFragment())
                "Body Mass Index" -> loadFragment(BodyMassIndexFragment())
                "Shapes" -> loadFragment(ShapesFragment())
                "Equation" -> loadFragment(MathEquationSolverFragment())
                "Currency Converter" -> loadFragment(CurrencyConverterFragment())
                "Temperature" -> loadFragment(TemperatureFragment())
                "Bodies" -> loadFragment(BodiesFragment())
                // Add more fragments as needed
                else -> loadFragment(BasicCalculatorFragment()) // Default fragment
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.calculatorFragmentContainer, fragment)
        transaction.commit()
    }

    private fun runAds() {

        if (NetworkUtil.isNetworkAvailable(this)) {
            MobileAds.initialize(this)
            adRequest = AdRequest.Builder().build()
            loadBanner()
        } else {
            // Handle the case where there's no network available
            Log.d("NetworkCheck", "No internet connection available.")
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun interstitialAd() {

        if (::adRequest.isInitialized) {
            InterstitialAd.load(this,
                AdConfig.getInterstitialAdId(),
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interstitialAd1 = interstitialAd
                        interstitialAd.show(this@CalculatorActivity)
                    }
                })
        }

    }


    override fun onStop() {
        super.onStop()
        // its much better this way
        interstitialAd()
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


}