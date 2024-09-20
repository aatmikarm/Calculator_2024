package com.aatmik.calculator.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.ActivityCalculatorBinding
import com.aatmik.calculator.fragment.BasicCalculatorFragment
import com.aatmik.calculator.fragment.ScientificCalculatorFragment
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
        setUpCalculator()
        floatingActionButton()
    }

    private fun setUpCalculator() {
        if (supportFragmentManager.findFragmentById(R.id.calculatorFragmentContainer) == null) {
            loadBasicCalculatorFragment()
        }
    }

    private fun runAds() {
        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        bannerAd()
        interstitialAd()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private var isScientificCalculatorOpen = false

    fun floatingActionButton() {
        // Set up FloatingActionButton to toggle between calculators
        binding.scientificCalculatorButton.setOnClickListener {
            if (isScientificCalculatorOpen) {
                loadBasicCalculatorFragment()
            } else {
                loadScientificCalculatorFragment()
            }
            // Toggle the state
            isScientificCalculatorOpen = !isScientificCalculatorOpen
        }
    }

    private fun loadScientificCalculatorFragment() {
        val fragment = ScientificCalculatorFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.calculatorFragmentContainer, fragment)
        transaction.commit()
    }

    private fun loadBasicCalculatorFragment() {
        val fragment = BasicCalculatorFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.calculatorFragmentContainer, fragment)
        transaction.commit()
    }

    private fun showInterstitialAd() {
        interstitialAd1.show(this@CalculatorActivity)
    }

    private fun interstitialAd() {
        InterstitialAd.load(
            this,
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

    private fun bannerAd() {
        binding.bannerAd1.loadAd(adRequest)
    }


}