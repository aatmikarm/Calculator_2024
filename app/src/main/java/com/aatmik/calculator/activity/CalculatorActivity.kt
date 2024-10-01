package com.aatmik.calculator.activity

import android.os.Bundle
import android.util.Log
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
import com.aatmik.calculator.fragment.TipFragment
import com.aatmik.calculator.fragment.WeightFragment
import com.aatmik.calculator.fragment.shapes.ShapesFragment
import com.aatmik.calculator.util.NetworkUtil
import com.example.yourapp.MathEquationSolverFragment
import com.google.android.gms.ads.AdRequest
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
        //runAds()
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
            bannerAd()
        } else {
            // Handle the case where there's no network available
            Log.d("NetworkCheck", "No internet connection available.")
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showInterstitialAd() {
        interstitialAd1.show(this@CalculatorActivity)
    }

    private fun interstitialAd() {
        val prod = "ca-app-pub-5678552217308395/9237780167"
        val test = "ca-app-pub-3940256099942544/1033173712"

        if (::adRequest.isInitialized) {
            InterstitialAd.load(this,
                test,
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

    private fun bannerAd() {
        if (::adRequest.isInitialized) {
            binding.bannerAd1.loadAd(adRequest)
        }
    }


}