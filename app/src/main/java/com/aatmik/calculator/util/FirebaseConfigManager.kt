package com.aatmik.calculator.util

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object FirebaseConfigManager {

    private const val TAG = "FirebaseRemoteConfig"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        Log.d(TAG, "Initializing FirebaseRemoteConfig instance")
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()

            Log.d(TAG, "Setting minimum fetch interval to 3600 seconds (1 hour)")
            setConfigSettingsAsync(configSettings)

            val defaults = getDefaultConfig()
            Log.d(TAG, "Setting default config values: $defaults")
            setDefaultsAsync(defaults)
        }
    }

    fun initialize(onComplete: ((Boolean) -> Unit)? = null) {
        Log.d(TAG, "initialize() called - Starting fetch and activate")

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val updated = task.result
                Log.d(TAG, "Fetch and activate SUCCESS - Config updated: $updated")

                Log.d(TAG, "Current Remote Config values:")
                Log.d(TAG, "  show_banner_ads = ${remoteConfig.getBoolean("show_banner_ads")}")
                Log.d(TAG, "  show_interstitial_ads = ${remoteConfig.getBoolean("show_interstitial_ads")}")
                Log.d(TAG, "  interstitial_frequency = ${remoteConfig.getLong("interstitial_frequency")}")
                Log.d(TAG, "  ad_cooldown_minutes = ${remoteConfig.getLong("ad_cooldown_minutes")}")
                Log.d(TAG, "  enable_voice_input = ${remoteConfig.getBoolean("enable_voice_input")}")

                Log.d(TAG, "Last fetch time: ${remoteConfig.info.fetchTimeMillis}")
                Log.d(TAG, "Last fetch status: ${remoteConfig.info.lastFetchStatus}")
            } else {
                Log.e(TAG, "Fetch and activate FAILED")
                Log.e(TAG, "Error: ${task.exception?.message}")
                Log.d(TAG, "Using default values instead")
            }

            onComplete?.invoke(task.isSuccessful)
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val value = remoteConfig.getString(key).ifEmpty { defaultValue }
        Log.d(TAG, "getString('$key') = '$value' (default: '$defaultValue')")
        return value
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = remoteConfig.getBoolean(key)
        Log.d(TAG, "getBoolean('$key') = $value (default: $defaultValue)")
        return value
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = remoteConfig.getLong(key).toInt()
        Log.d(TAG, "getInt('$key') = $value (default: $defaultValue)")
        return value
    }

    private fun getDefaultConfig(): Map<String, Any> {
        return mapOf(
            "show_banner_ads" to true,
            "show_interstitial_ads" to true,
            "interstitial_frequency" to 3,
            "ad_cooldown_minutes" to 5,
            "enable_voice_input" to true
        )
    }

    fun shouldShowBannerAds(): Boolean {
        val value = getBoolean("show_banner_ads", true)
        Log.d(TAG, "shouldShowBannerAds() returning: $value")
        return value
    }

    fun shouldShowInterstitialAds(): Boolean {
        val value = getBoolean("show_interstitial_ads", true)
        Log.d(TAG, "shouldShowInterstitialAds() returning: $value")
        return value
    }

    fun getInterstitialFrequency(): Int {
        val value = getInt("interstitial_frequency", 3)
        Log.d(TAG, "getInterstitialFrequency() returning: $value")
        return value
    }

    fun getAdCooldownMinutes(): Int {
        val value = getInt("ad_cooldown_minutes", 5)
        Log.d(TAG, "getAdCooldownMinutes() returning: $value")
        return value
    }

    fun isVoiceInputEnabled(): Boolean {
        val value = getBoolean("enable_voice_input", true)
        Log.d(TAG, "isVoiceInputEnabled() returning: $value")
        return value
    }
}