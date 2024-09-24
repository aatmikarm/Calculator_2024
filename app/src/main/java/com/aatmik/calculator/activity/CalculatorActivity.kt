package com.aatmik.calculator.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.ActivityCalculatorBinding
import com.aatmik.calculator.fragment.AgeFragment
import com.aatmik.calculator.fragment.BasicCalculatorFragment
import com.aatmik.calculator.fragment.CounterFragment
import com.aatmik.calculator.fragment.LengthFragment
import com.aatmik.calculator.fragment.PercentageFragment
import com.aatmik.calculator.fragment.StopwatchFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

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
        setUpCalculator(calculatorType)
    }

    private fun setUpCalculator(calculatorType: String?) {
        if (supportFragmentManager.findFragmentById(R.id.calculatorFragmentContainer) == null) {
            when (calculatorType) {
                "Basic" -> loadFragment(BasicCalculatorFragment())
                "Convertor" -> loadFragment(CounterFragment())
                "Stopwatch" -> loadFragment(StopwatchFragment())
                "Percentage" -> loadFragment(PercentageFragment())
                "Age" -> loadFragment(AgeFragment())
                "Length" -> loadFragment(LengthFragment())
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
        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        bannerAd()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showInterstitialAd() {
        interstitialAd1.show(this@CalculatorActivity)
    }

    private fun interstitialAd() {
        if (::adRequest.isInitialized) {
            InterstitialAd.load(this,
                "ca-app-pub-3940256099942544/1033173712",
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