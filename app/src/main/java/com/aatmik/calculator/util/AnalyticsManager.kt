package com.aatmik.calculator.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Centralized Analytics Manager for Firebase Analytics with Logcat debugging
 *
 * Usage:
 * - Initialize once in Application or MainActivity: AnalyticsManager.init(context)
 * - Log events anywhere: AnalyticsManager.log("event_name")
 * - Log events with params: AnalyticsManager.log("event_name", "key" to "value")
 *
 * To filter in Logcat: Search for "Analytics" tag
 */
object AnalyticsManager {

    private const val TAG = "AnalyticsManager" // Tag for Logcat filtering
    private var analytics: FirebaseAnalytics? = null
    private var isEnabled = true // Can be used to disable analytics in debug builds
    private var debugMode = true // Enable/disable Logcat logging

    /**
     * Initialize Firebase Analytics - Call this once in Application class or MainActivity
     */
    fun init(context: Context) {
        if (analytics == null) {
            analytics = Firebase.analytics
            //analytics = FirebaseAnalytics.getInstance(context)

            Log.d(TAG, "✅ Firebase Analytics initialized successfully")

            // Optional: Set user properties, session timeout, etc.
            setDefaultUserProperties()
        } else {
            Log.d(TAG, "ℹ️ Firebase Analytics already initialized")
        }
    }

    /**
     * Log a simple event without parameters
     * Example: AnalyticsManager.log("button_clicked")
     */
    fun log(eventName: String) {
        if (!isEnabled) {
            Log.w(TAG, "⚠️ Analytics disabled - Event not sent: $eventName")
            return
        }

        val formattedName = eventName.toFirebaseFormat()
        analytics?.logEvent(formattedName, null)

        if (debugMode) {
            Log.d(TAG, "📊 Event logged: '$formattedName'")
        }
    }

    /**
     * Log an event with single parameter
     * Example: AnalyticsManager.log("calculator_used", "type" to "basic")
     */
    fun log(eventName: String, param: Pair<String, Any>) {
        if (!isEnabled) {
            Log.w(TAG, "⚠️ Analytics disabled - Event not sent: $eventName")
            return
        }

        val formattedName = eventName.toFirebaseFormat()
        analytics?.logEvent(formattedName) {
            when (param.second) {
                is String -> param(param.first, param.second as String)
                is Long -> param(param.first, param.second as Long)
                is Double -> param(param.first, param.second as Double)
                else -> param(param.first, param.second.toString())
            }
        }

        if (debugMode) {
            Log.d(TAG, "📊 Event logged: '$formattedName' | Param: {${param.first}=${param.second}}")
        }
    }

    /**
     * Log an event with multiple parameters
     * Example: AnalyticsManager.log("purchase_made", "item" to "premium", "price" to 9.99)
     */
    fun log(eventName: String, vararg params: Pair<String, Any>) {
        if (!isEnabled) {
            Log.w(TAG, "⚠️ Analytics disabled - Event not sent: $eventName")
            return
        }

        val formattedName = eventName.toFirebaseFormat()
        analytics?.logEvent(formattedName) {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    else -> param(key, value.toString())
                }
            }
        }

        if (debugMode) {
            val paramsString = params.joinToString(", ") { "${it.first}=${it.second}" }
            Log.d(TAG, "📊 Event logged: '$formattedName' | Params: {$paramsString}")
        }
    }

    /**
     * Log an event with Bundle (for complex parameters)
     * Example: AnalyticsManager.logWithBundle("complex_event", bundle)
     */
    fun logWithBundle(eventName: String, bundle: Bundle) {
        if (!isEnabled) {
            Log.w(TAG, "⚠️ Analytics disabled - Event not sent: $eventName")
            return
        }

        val formattedName = eventName.toFirebaseFormat()
        analytics?.logEvent(formattedName, bundle)

        if (debugMode) {
            Log.d(TAG, "📊 Event logged: '$formattedName' | Bundle size: ${bundle.size()}")
        }
    }

    // ========== PREDEFINED EVENTS FOR YOUR CALCULATOR APP ==========

    /**
     * Log when a specific calculator is opened
     */
    fun logCalculatorOpened(calculatorName: String) {
        log("calculator_opened", "calculator_name" to calculatorName)
        Log.i(TAG, "🧮 Calculator opened: $calculatorName")
    }

    /**
     * Log when a calculation is performed
     */
    fun logCalculationPerformed(calculatorType: String, operation: String? = null) {
        if (operation != null) {
            log("calculation_performed",
                "calculator_type" to calculatorType,
                "operation" to operation
            )
            Log.i(TAG, "➗ Calculation performed: $calculatorType - $operation")
        } else {
            log("calculation_performed", "calculator_type" to calculatorType)
            Log.i(TAG, "➗ Calculation performed: $calculatorType")
        }
    }

    /**
     * Log when user shares the app
     */
    fun logAppShared(method: String = "unknown") {
        log("app_shared", "share_method" to method)
        Log.i(TAG, "📤 App shared via: $method")
    }

    /**
     * Log theme change
     */
    fun logThemeChanged(themeName: String) {
        log("theme_changed", "theme_name" to themeName)
        Log.i(TAG, "🎨 Theme changed to: $themeName")
    }

    /**
     * Log ad events
     */
    fun logAdEvent(eventType: String, adType: String = "banner") {
        log("ad_event",
            "event_type" to eventType,
            "ad_type" to adType
        )
        Log.i(TAG, "📢 Ad event: $eventType ($adType)")
    }

    /**
     * Log screen view (for tracking which screens users visit most)
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }

        val classInfo = screenClass?.let { " [$it]" } ?: ""
        Log.i(TAG, "📱 Screen viewed: $screenName$classInfo")
    }

    /**
     * Log app open event
     */
    fun logAppOpen() {
        analytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        Log.i(TAG, "🚀 App opened ${analytics.toString()}")
    }

    /**
     * Log search event
     */
    fun logSearch(searchTerm: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, searchTerm)
        }
        Log.i(TAG, "🔍 Search performed: '$searchTerm'")
    }

    // ========== USER PROPERTIES ==========

    /**
     * Set a user property
     * Example: AnalyticsManager.setUserProperty("favorite_calculator", "scientific")
     */
    fun setUserProperty(name: String, value: String?) {
        analytics?.setUserProperty(name, value)
        Log.d(TAG, "👤 User property set: $name = $value")
    }

    /**
     * Set user's preferred calculator
     */
    fun setPreferredCalculator(calculatorName: String) {
        setUserProperty("preferred_calculator", calculatorName)
        Log.i(TAG, "⭐ Preferred calculator set: $calculatorName")
    }

    /**
     * Set user type (free/premium)
     */
    fun setUserType(isPremium: Boolean) {
        val userType = if (isPremium) "premium" else "free"
        setUserProperty("user_type", userType)
        Log.i(TAG, "💎 User type set: $userType")
    }

    private fun setDefaultUserProperties() {
        // Set any default user properties here
        setUserProperty("app_version", "1.0.0") // Get from BuildConfig
        Log.d(TAG, "📝 Default user properties set")
    }

    // ========== UTILITY FUNCTIONS ==========

    /**
     * Enable or disable analytics (useful for debug builds or user preferences)
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        analytics?.setAnalyticsCollectionEnabled(enabled)
        Log.w(TAG, if (enabled) "✅ Analytics ENABLED" else "🔴 Analytics DISABLED")
    }

    /**
     * Enable or disable debug logging
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        Log.d(TAG, if (enabled) "🔧 Debug mode ON" else "🔧 Debug mode OFF")
    }

    /**
     * Convert event names to Firebase format (lowercase with underscores)
     * Example: "Button Clicked" -> "button_clicked"
     */
    private fun String.toFirebaseFormat(): String {
        val formatted = this.replace(" ", "_")
            .replace("-", "_")
            .lowercase()
            .take(40) // Firebase event names max 40 chars

        // Log warning if name was truncated
        if (this.length > 40) {
            Log.w(TAG, "⚠️ Event name truncated: '$this' -> '$formatted'")
        }

        return formatted
    }

    /**
     * Check if analytics is initialized
     */
    fun isInitialized(): Boolean {
        val initialized = analytics != null
        if (!initialized) {
            Log.e(TAG, "❌ Analytics not initialized! Call init() first")
        }
        return initialized
    }

    /**
     * Log debug info about current state
     */
    fun logDebugInfo() {
        Log.d(TAG, "========== Analytics Debug Info ==========")
        Log.d(TAG, "Initialized: ${analytics != null}")
        Log.d(TAG, "Enabled: $isEnabled")
        Log.d(TAG, "Debug Mode: $debugMode")
        Log.d(TAG, "==========================================")
    }
}