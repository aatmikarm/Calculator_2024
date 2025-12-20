package com.aatmik.calculator

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aatmik.calculator.util.AdFrequencyManager
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.FirebaseConfigManager
import com.aatmik.calculator.util.SubscriptionManager
import com.google.firebase.FirebaseApp

class CalculatorApplication : Application(), Application.ActivityLifecycleCallbacks {

    private var sessionStartTime: Long = 0
    private var isAppInForeground = false

    companion object {
        private const val TAG = "CalculatorApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Application onCreate started")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialized")

        FirebaseConfigManager.initialize { success ->
            Log.d(TAG, "Remote Config loaded: $success")
            // Update AdConfig based on remote values
            updateAdConfigFromRemote()
        }

        // Initialize Analytics
        AnalyticsManager.init(this)
        Log.d(TAG, "Analytics initialized")

        // **NEW: Initialize Subscription Manager - MUST BE BEFORE OTHER INITIALIZATIONS**
        SubscriptionManager.init(this)
        Log.d(TAG, "Subscription Manager initialized")

        // Register lifecycle callbacks
        registerActivityLifecycleCallbacks(this)
        Log.d(TAG, "Activity lifecycle callbacks registered")

        // Observe app lifecycle (foreground/background)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        Log.d(TAG, "App lifecycle observer registered")
    }

    inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            if (!isAppInForeground) {
                isAppInForeground = true
                sessionStartTime = System.currentTimeMillis()
                Log.d(TAG, "App entered foreground - Session started")
                AnalyticsManager.log("app_opened")
                AdFrequencyManager.resetBasicCalculatorCount(applicationContext)
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            if (isAppInForeground) {
                isAppInForeground = false
                val sessionDuration = System.currentTimeMillis() - sessionStartTime
                val durationSeconds = sessionDuration / 1000
                val durationMinutes = durationSeconds / 60

                Log.d(TAG, "App entered background - Session ended")
                Log.d(TAG, "Session duration: ${durationMinutes}m ${durationSeconds % 60}s ($durationSeconds seconds)")

                // Log session duration
                AnalyticsManager.log(
                    "session_duration",
                    "seconds" to durationSeconds,
                    "minutes" to durationMinutes
                )
            }
        }
    }

    // Activity Lifecycle Callbacks (for debugging)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "Activity Created: ${activity.localClassName}")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "Activity Started: ${activity.localClassName}")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "Activity Resumed: ${activity.localClassName}")
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "Activity Paused: ${activity.localClassName}")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "Activity Stopped: ${activity.localClassName}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d(TAG, "Activity SaveInstanceState: ${activity.localClassName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "Activity Destroyed: ${activity.localClassName}")
    }

    private fun updateAdConfigFromRemote() {
        // This will be called after Remote Config loads
        Log.d(TAG, "Updating ad settings from Remote Config")
    }
}