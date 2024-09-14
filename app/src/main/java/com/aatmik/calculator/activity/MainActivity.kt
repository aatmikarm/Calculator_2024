package com.aatmik.calculator.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adRequest: AdRequest
    private lateinit var interstitialAd1: InterstitialAd


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()

//        val actionBar = supportActionBar
//        actionBar!!.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        bannerAd()
        interstitialAd()

        binding.apply {
            btCalculator.setOnClickListener {
                startActivity(Intent(this@MainActivity, CalculatorActivity::class.java))
            }

            btConverter.setOnClickListener {
                startActivity(Intent(this@MainActivity, UnitConverterActivity::class.java))
            }

            btTimer.setOnClickListener {
                startActivity(Intent(this@MainActivity, TimerActivity::class.java))
            }
        }

    }

    private fun showInterstitialAd() {
        interstitialAd1.show(this@MainActivity)
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
                }
            })

    }

    private fun bannerAd() {
        binding.bannerAd1.loadAd(adRequest)
    }

}