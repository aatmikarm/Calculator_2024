package com.aatmik.calculator.util

import android.content.Context
import android.util.Log

class AdFrequencyManager {

    companion object {
        private const val PREFS_NAME = "ad_frequency_prefs"
        private const val KEY_CALCULATOR_USAGE_COUNT = "calculator_usage_count"
        private const val KEY_LAST_AD_SHOWN_SESSION = "last_ad_shown_session"
        private const val USAGE_THRESHOLD = 1 // Show ad after every 3 or x no of calculator usages
        private const val AD_COOLDOWN_SECONDS = 10 // Configurable cooldown period in seconds

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

            // Get current usage count
            val currentCount = prefs.getInt(KEY_CALCULATOR_USAGE_COUNT, 0)

            // Check if we've reached the threshold
            if (currentCount >= USAGE_THRESHOLD) {
                // Reset the counter for next cycle
                prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, 0).apply()
                Log.d("AdFrequency", "Threshold reached ($currentCount >= $USAGE_THRESHOLD). Showing ad and resetting counter.")
                return true
            }

            Log.d("AdFrequency", "Threshold not reached ($currentCount < $USAGE_THRESHOLD). Ad not shown.")
            return false
        }

        /**
         * Reset the usage counter (useful for testing or specific scenarios)
         */
        fun resetUsageCounter(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_CALCULATOR_USAGE_COUNT, 0).apply()
            Log.d("AdFrequency", "Usage counter reset to 0")
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
            val cooldownMillis = AD_COOLDOWN_SECONDS * 1000L

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

        /**
         * Get remaining usage count until next ad
         */
        fun getRemainingUsageUntilAd(context: Context): Int {
            val currentCount = getCurrentUsageCount(context)
            return maxOf(0, USAGE_THRESHOLD - currentCount)
        }

        /**
         * Get configured cooldown period in seconds
         */
        fun getCooldownSeconds(): Int {
            return AD_COOLDOWN_SECONDS
        }

        /**
         * Check if cooldown is currently active
         */
        fun isCooldownActive(context: Context): Boolean {
            return wasAdRecentlyShown(context)
        }

        /**
         * Get debug information about current state
         */
        fun getDebugInfo(context: Context): String {
            val currentCount = getCurrentUsageCount(context)
            val remaining = getRemainingUsageUntilAd(context)
            val cooldownActive = isCooldownActive(context)

            return """
                Usage Count: $currentCount/$USAGE_THRESHOLD
                Remaining until ad: $remaining
                Cooldown active: $cooldownActive
                Cooldown period: ${AD_COOLDOWN_SECONDS}s
            """.trimIndent()
        }
    }
}