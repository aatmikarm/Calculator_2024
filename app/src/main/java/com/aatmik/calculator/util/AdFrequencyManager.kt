package com.aatmik.calculator.util

import android.content.Context
import android.util.Log

class AdFrequencyManager {

    companion object {
        private const val PREFS_NAME = "ad_frequency_prefs"
        private const val KEY_CALCULATOR_USAGE_COUNT = "calculator_usage_count"
        private const val KEY_LAST_AD_SHOWN_SESSION = "last_ad_shown_session"
        private const val KEY_BASIC_CALC_CALCULATION_COUNT = "basic_calc_calculation_count"
        private const val KEY_BASIC_CALC_LAST_AD_TIME = "basic_calc_last_ad_time"

        private const val BASIC_CALC_CALCULATION_THRESHOLD = 10
        private const val BASIC_CALC_MIN_TIME_BETWEEN_ADS_SECONDS = 120

        /**
         * Track calculator usage - call this when a calculator is actually used
         * @param context Application context
         * @param calculatorName Name of the calculator being used
         */
        fun trackCalculatorUsage(context: Context, calculatorName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Get current usage count and increment it
            val currentCount = prefs.getInt(KEY_CALCULATOR_USAGE_COUNT, 0)
            val newCount = currentCount + 1

            // Save the incremented count
            prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, newCount).apply()

            Log.d("AdFrequency", "Calculator '$calculatorName' used. Usage count: $newCount")
        }

        /**
         * Check if interstitial ad should be shown based on usage count
         * @param context Application context
         * @return true if ad should be shown, false otherwise
         */
        fun shouldShowInterstitialAd(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(KEY_CALCULATOR_USAGE_COUNT, 0)
            val threshold = FirebaseConfigManager.getInterstitialFrequency()

            val reached = currentCount >= threshold
            Log.d("AdFrequency", "Threshold check: $currentCount >= $threshold -> $reached")
            return reached
        }

        /**
         * Call this ONLY after the interstitial ad has actually been shown (ad.show() executed).
         * This consumes the usage count — not the check.
         */
        fun onInterstitialActuallyShown(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, 0).apply()
            Log.d("AdFrequency", "Ad actually shown - counter reset")
        }

        fun trackBasicCalculatorCalculation(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(KEY_BASIC_CALC_CALCULATION_COUNT, 0)
            val newCount = currentCount + 1
            prefs.edit().putInt(KEY_BASIC_CALC_CALCULATION_COUNT, newCount).apply()
            Log.d("AdFrequency", "Basic calculator calculation performed. Count: $newCount")
        }

        fun shouldShowBasicCalculatorInterstitial(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val calculationCount = prefs.getInt(KEY_BASIC_CALC_CALCULATION_COUNT, 0)
            if (calculationCount < BASIC_CALC_CALCULATION_THRESHOLD) {
                Log.d("AdFrequency", "Basic calc: Not enough calculations ($calculationCount < $BASIC_CALC_CALCULATION_THRESHOLD)")
                return false
            }

            val lastAdTime = prefs.getLong(KEY_BASIC_CALC_LAST_AD_TIME, 0)
            val currentTime = System.currentTimeMillis()
            val timeSinceLastAd = (currentTime - lastAdTime) / 1000

            if (lastAdTime > 0 && timeSinceLastAd < BASIC_CALC_MIN_TIME_BETWEEN_ADS_SECONDS) {
                val remaining = BASIC_CALC_MIN_TIME_BETWEEN_ADS_SECONDS - timeSinceLastAd
                Log.d("AdFrequency", "Basic calc: Too soon since last ad. Wait ${remaining}s more")
                return false
            }

            Log.d("AdFrequency", "Basic calc: Eligible to show after $calculationCount calculations")
            return true
        }

        /**
         * Call this ONLY after the interstitial ad has actually been shown for Basic Calculator.
         */
        fun onBasicCalcInterstitialActuallyShown(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_BASIC_CALC_CALCULATION_COUNT, 0)
                .putLong(KEY_BASIC_CALC_LAST_AD_TIME, System.currentTimeMillis())
                .apply()
            Log.d("AdFrequency", "Basic calc ad actually shown - counter reset")
        }

        fun resetBasicCalculatorCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_BASIC_CALC_CALCULATION_COUNT, 0).apply()
            Log.d("AdFrequency", "Basic calculator count reset on new app session")
        }

        /**
         * Mark that an ad was shown in current session to prevent showing multiple ads
         * in quick succession if user navigates back and forth rapidly
         */
        fun markAdShownInSession(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_AD_SHOWN_SESSION, currentTime).apply()
            Log.d("AdFrequency", "Ad shown timestamp saved: $currentTime")
        }

        /**
         * Check if ad was recently shown to prevent spam
         * Configurable cooldown period to balance user experience and ad frequency
         */
        fun wasAdRecentlyShown(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastAdTime = prefs.getLong(KEY_LAST_AD_SHOWN_SESSION, 0)
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - lastAdTime

            // **NEW: Get cooldown from Remote Config (convert minutes to milliseconds)**
            val cooldownMillis = FirebaseConfigManager.getAdCooldownMinutes() * 60 * 1000L

            val isRecentlyShown = timeDifference < cooldownMillis

            if (isRecentlyShown) {
                val secondsAgo = timeDifference / 1000
                val remainingCooldown = (cooldownMillis - timeDifference) / 1000
                Log.d("AdFrequency", "Ad recently shown $secondsAgo seconds ago. Waiting $remainingCooldown more seconds.")
            } else if (lastAdTime > 0) {
                Log.d("AdFrequency", "Cooldown period passed. Ready to show ad.")
            }

            return isRecentlyShown
        }
    }
}