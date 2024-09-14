package com.aatmik.calculator.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.ViewPagerAdapter
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

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()
        bannerAd()
        interstitialAd()

        /*    val actionBar = supportActionBar
            actionBar!!.title = getString(R.string.calculator)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    */
        setUpTabs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setUpTabs() {
        binding.apply {
            val adapter = ViewPagerAdapter(supportFragmentManager)
            adapter.addFragment(BasicCalculatorFragment(), getString(R.string.basic_calculator))
            adapter.addFragment(
                ScientificCalculatorFragment(),
                getString(R.string.scientific_calculator)
            )
            viewPager.adapter = adapter
            tabLayout.setupWithViewPager(viewPager)
        }

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