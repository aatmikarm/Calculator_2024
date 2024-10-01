package com.aatmik.calculator.util

object AdConfig {
    // Toggle between true (test ads) and false (production ads)
    private const val USE_TEST_ADS = true

    // Production Ad IDs
    private const val PROD_BANNER_AD_ID = "ca-app-pub-5678552217308395/1592290092"
    private const val PROD_INTERSTITIAL_AD_ID = "ca-app-pub-5678552217308395/9237780167"

    // Test Ad IDs (Google's test ad units)
    private const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    // Method to get the correct Banner Ad ID based on the environment
    fun getBannerAdId(): String {
        return if (USE_TEST_ADS) TEST_BANNER_AD_ID else PROD_BANNER_AD_ID
    }

    // Method to get the correct Interstitial Ad ID based on the environment
    fun getInterstitialAdId(): String {
        return if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_ID else PROD_INTERSTITIAL_AD_ID
    }
}
