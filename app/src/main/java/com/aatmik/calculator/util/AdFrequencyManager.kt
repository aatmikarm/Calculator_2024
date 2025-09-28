package com.aatmik.calculator.util

import android.content.Context

class AdFrequencyManager {

    companion object {
        private const val PREFS_NAME = "ad_frequency_prefs"
        private const val KEY_CALCULATOR_USAGE_COUNT = "calculator_usage_count"
        private const val KEY_LAST_AD_SHOWN_SESSION = "last_ad_shown_session"
        private const val USAGE_THRESHOLD = 3 // Show ad after every 3 calculator usages

        /**
         * Track calculator usage and determine if interstitial ad should be shown
         * @param context Application context
         * @return true if ad should be shown, false otherwise
         */
        fun shouldShowInterstitialAd(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Get current usage count
            val currentCount = prefs.getInt(KEY_CALCULATOR_USAGE_COUNT, 0)
            val newCount = currentCount + 1

            // Save the incremented count
            prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, newCount).apply()

            // Check if we've reached the threshold
            if (newCount >= USAGE_THRESHOLD) {
                // Reset the counter for next cycle
                prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, 0).apply()
                return true
            }

            return false
        }

        /**
         * Reset the usage counter (useful for testing or specific scenarios)
         */
        fun resetUsageCounter(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, 0).apply()
        }

        /**
         * Get current usage count (useful for debugging or showing to user)
         */
        fun getCurrentUsageCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_CALCULATOR_USAGE_COUNT, 0)
        }

        /**
         * Mark that an ad was shown in current session to prevent showing multiple ads
         * in quick succession if user navigates back and forth rapidly
         */
        fun markAdShownInSession(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_AD_SHOWN_SESSION, currentTime).apply()
        }

        /**
         * Check if ad was recently shown (within last 30 seconds) to prevent spam
         */
        fun wasAdRecentlyShown(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastAdTime = prefs.getLong(KEY_LAST_AD_SHOWN_SESSION, 0)
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - lastAdTime

            // Return true if ad was shown within last 30 seconds
            return timeDifference < 30000
        }

        /**
         * Get remaining usage count until next ad
         */
        fun getRemainingUsageUntilAd(context: Context): Int {
            val currentCount = getCurrentUsageCount(context)
            return USAGE_THRESHOLD - currentCount
        }
    }
}