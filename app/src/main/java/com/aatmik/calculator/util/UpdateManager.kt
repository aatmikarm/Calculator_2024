package com.aatmik.calculator.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.util.Log
import com.aatmik.calculator.databinding.UpdateDialogLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class UpdateManager {

    companion object {
        private const val TAG = "UpdateManager"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_SKIPPED_VERSION = "skipped_version"
        private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
        private const val PLAY_STORE_BASE_URL = "https://play.google.com/store/apps/details?id="
        private const val PACKAGE_NAME = "com.aatmik.calculator"

        /**
         * Check for app updates automatically (called on app start)
         * This will only check once every 24 hours to avoid spam
         */
        fun checkForUpdatesAutomatically(context: Context) {
            Log.d(TAG, "checkForUpdatesAutomatically: Starting automatic update check")
            if (context !is AppCompatActivity) {
                Log.w(TAG, "checkForUpdatesAutomatically: Context is not AppCompatActivity, returning")
                return
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
            val currentTime = System.currentTimeMillis()

            Log.d(TAG, "checkForUpdatesAutomatically: Last check time: $lastCheckTime, Current time: $currentTime")

            // Check if 24 hours have passed since last check
            if (currentTime - lastCheckTime > CHECK_INTERVAL) {
                Log.i(TAG, "checkForUpdatesAutomatically: 24 hours passed, checking for updates")
                checkForUpdates(context, isManualCheck = false)
            } else {
                val timeRemaining = CHECK_INTERVAL - (currentTime - lastCheckTime)
                Log.d(TAG, "checkForUpdatesAutomatically: Still within 24 hour interval, ${timeRemaining / (1000 * 60 * 60)} hours remaining")
            }
        }

        /**
         * Check for app updates manually (called when user clicks update button)
         */
        fun checkForUpdatesManually(context: Context) {
            Log.d(TAG, "checkForUpdatesManually: Starting manual update check")
            if (context !is AppCompatActivity) {
                Log.w(TAG, "checkForUpdatesManually: Context is not AppCompatActivity, returning")
                return
            }
            checkForUpdates(context, isManualCheck = true)
        }

        private fun checkForUpdates(context: AppCompatActivity, isManualCheck: Boolean) {
            Log.d(TAG, "checkForUpdates: Starting update check, isManualCheck: $isManualCheck")
            context.lifecycleScope.launch {
                try {
                    val currentVersion = getCurrentAppVersion(context)
                    Log.i(TAG, "checkForUpdates: Current app version: $currentVersion")

                    Log.d(TAG, "checkForUpdates: Fetching Play Store version...")
                    val playStoreVersion = getPlayStoreVersion()
                    Log.i(TAG, "checkForUpdates: Play Store version: $playStoreVersion")

                    // Update last check time
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                    Log.d(TAG, "checkForUpdates: Updated last check time")

                    if (playStoreVersion != null && isUpdateAvailable(currentVersion, playStoreVersion)) {
                        Log.i(TAG, "checkForUpdates: Update available! Current: $currentVersion, Available: $playStoreVersion")

                        // Check if user has skipped this version
                        val skippedVersion = prefs.getString(KEY_SKIPPED_VERSION, "")
                        Log.d(TAG, "checkForUpdates: Skipped version: $skippedVersion")

                        if (skippedVersion != playStoreVersion || isManualCheck) {
                            Log.d(TAG, "checkForUpdates: Showing update dialog")
                            withContext(Dispatchers.Main) {
                                showUpdateDialog(context, currentVersion, playStoreVersion)
                            }
                        } else {
                            Log.d(TAG, "checkForUpdates: Version already skipped by user, not showing dialog")
                        }
                    } else if (playStoreVersion == null && isManualCheck) {
                        // Fallback mechanism: Open Play Store when version detection fails for manual checks
                        Log.w(TAG, "checkForUpdates: Version detection failed for manual check, showing fallback dialog")
                        withContext(Dispatchers.Main) {
                            //showVersionDetectionFailedDialog(context)
                            openPlayStore(context)
                        }
                    } else if (isManualCheck) {
                        Log.d(TAG, "checkForUpdates: No updates available for manual check")
                        // Show "no updates available" only for manual checks
                        withContext(Dispatchers.Main) {
                            showNoUpdateDialog(context)
                        }
                    } else {
                        Log.d(TAG, "checkForUpdates: No updates available for automatic check")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "checkForUpdates: Error occurred during update check", e)
                    if (isManualCheck) {
                        withContext(Dispatchers.Main) {
                            showUpdateCheckErrorDialog(context)
                        }
                    }
                }
            }
        }

        private suspend fun getPlayStoreVersion(): String? = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getPlayStoreVersion: Starting Play Store version fetch")
                val url = "$PLAY_STORE_BASE_URL$PACKAGE_NAME"
                Log.d(TAG, "getPlayStoreVersion: Connecting to URL: $url")

                val document: Document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get()

                Log.d(TAG, "getPlayStoreVersion: Successfully connected to Play Store page")

                // Method 1: Look for version in app description (YOUR CUSTOM VERSION SECTION)
                Log.d(TAG, "getPlayStoreVersion: Searching for custom version section in description")
                val pageText = document.text()

                // Look for your custom version patterns in description
                val versionPatterns = listOf(
                    // Format: "Version Name: 8.0" or "Version: 8.0"
                    Regex("""Version\s*(?:Name|Code)?\s*:?\s*(\d+\.\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
                    // Format: "Current Version: 8.0"
                    Regex("""Current\s*Version\s*:?\s*(\d+\.\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
                    // Format: "App Version: 8.0"
                    Regex("""App\s*Version\s*:?\s*(\d+\.\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
                    // Format: "v8.0" or "V8.0"
                    Regex("""[vV](\d+\.\d+(?:\.\d+)?)"""),
                    // Format: "Version Code: 8.0"
                    Regex("""Version\s*Code\s*:?\s*(\d+\.\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                )

                for (pattern in versionPatterns) {
                    val match = pattern.find(pageText)
                    if (match != null) {
                        val version = match.groupValues[1]
                        Log.i(TAG, "getPlayStoreVersion: Version found in description with pattern: $version")
                        return@withContext version
                    }
                }

                // Method 2: Look for version in structured description sections
                Log.d(TAG, "getPlayStoreVersion: Searching for version in description sections")
                val lines = pageText.split("\n", "\r\n", "\r", ".", "•", "✅", "🔢", "📱")

                for (line in lines) {
                    val trimmedLine = line.trim()
                    Log.d(TAG, "getPlayStoreVersion: Checking description line: '$trimmedLine'")

                    // Skip very long lines (likely not version info)
                    if (trimmedLine.length > 100) continue

                    // Look for lines that contain version-like patterns
                    for (pattern in versionPatterns) {
                        val match = pattern.find(trimmedLine)
                        if (match != null) {
                            val version = match.groupValues[1]
                            Log.i(TAG, "getPlayStoreVersion: Version found in description line: $version")
                            return@withContext version
                        }
                    }
                }

                // Method 3: Fallback - Look for standalone version numbers in reasonable context
                Log.d(TAG, "getPlayStoreVersion: Using fallback method - searching for version numbers near version keywords")

                // Split into smaller chunks around version-related keywords
                val versionKeywords = listOf("version", "updated", "current", "latest", "release")
                for (keyword in versionKeywords) {
                    val keywordIndex = pageText.indexOf(keyword, ignoreCase = true)
                    if (keywordIndex != -1) {
                        // Get text around the keyword (100 characters before and after)
                        val start = maxOf(0, keywordIndex - 100)
                        val end = minOf(pageText.length, keywordIndex + 100)
                        val contextText = pageText.substring(start, end)

                        Log.d(TAG, "getPlayStoreVersion: Checking context around '$keyword': '$contextText'")

                        // Look for version patterns in this context
                        val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?)""")
                        val matches = versionRegex.findAll(contextText).toList()

                        for (match in matches) {
                            val version = match.value
                            val parts = version.split(".")
                            val major = parts[0].toIntOrNull() ?: 0

                            // Accept versions that are likely app versions (not ratings or dates)
                            if (major >= 1 && !version.matches(Regex("""[1-5]\.0"""))) {
                                Log.i(TAG, "getPlayStoreVersion: Version found near keyword '$keyword': $version")
                                return@withContext version
                            }
                        }
                    }
                }

                // Method 4: Look for direct version elements (existing method as backup)
                Log.d(TAG, "getPlayStoreVersion: Searching for direct version elements as backup")
                val allElements = document.allElements
                for (element in allElements) {
                    val text = element.ownText()

                    if (text.matches(Regex("""\d+\.\d+(?:\.\d+)?"""))) {
                        val versionNumber = text
                        val parts = versionNumber.split(".")
                        val major = parts[0].toIntOrNull() ?: 0

                        // Skip obvious ratings (1.0-5.0 with single decimal)
                        if (parts.size == 2 && major in 1..5) {
                            continue
                        }

                        // Accept reasonable version numbers
                        if (major >= 6) { // Most apps are version 6.0 or higher
                            Log.i(TAG, "getPlayStoreVersion: Version found in element (backup method): $versionNumber")
                            return@withContext versionNumber
                        }
                    }
                }

                Log.w(TAG, "getPlayStoreVersion: No version found using any method")
                null
            } catch (e: Exception) {
                Log.e(TAG, "getPlayStoreVersion: Error fetching Play Store version", e)
                null
            }
        }

        private fun getCurrentAppVersion(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "1.0.0"
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0.0"
            }
        }

        private fun isUpdateAvailable(currentVersion: String, playStoreVersion: String): Boolean {
            return try {
                val current = parseVersion(currentVersion)
                val playStore = parseVersion(playStoreVersion)

                // Compare version numbers
                for (i in 0 until maxOf(current.size, playStore.size)) {
                    val currentPart = current.getOrNull(i) ?: 0
                    val playStorePart = playStore.getOrNull(i) ?: 0

                    when {
                        playStorePart > currentPart -> return true
                        playStorePart < currentPart -> return false
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }

        private fun parseVersion(version: String): List<Int> {
            return version.split(".").mapNotNull { it.toIntOrNull() }
        }

        private fun showUpdateDialog(context: AppCompatActivity, currentVersion: String, newVersion: String) {
            Log.d(TAG, "showUpdateDialog: Showing update dialog for version $currentVersion -> $newVersion")
            val binding = UpdateDialogLayoutBinding.inflate(LayoutInflater.from(context))

            // Set version information
            binding.tvCurrentVersion.text = "Current: v$currentVersion"
            binding.tvNewVersion.text = "Available: v$newVersion"

            val dialog = MaterialAlertDialogBuilder(context)
                .setView(binding.root)
                .setCancelable(true)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


            binding.btnClose.setOnClickListener {
                Log.d(TAG, "showUpdateDialog: User clicked close button")
                dialog.dismiss()
            }

            binding.btnSkip.setOnClickListener {
                Log.d(TAG, "showUpdateDialog: User clicked skip button for version $newVersion")
                // Save skipped version to preferences
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_SKIPPED_VERSION, newVersion).apply()
                dialog.dismiss()
            }

            binding.btnUpdateNow.setOnClickListener {
                Log.d(TAG, "showUpdateDialog: User clicked update now button")
                openPlayStore(context)
                dialog.dismiss()
            }

            dialog.show()
            Log.i(TAG, "showUpdateDialog: Update dialog displayed successfully")
        }

        private fun showVersionDetectionFailedDialog(context: Context) {
            Log.d(TAG, "showVersionDetectionFailedDialog: Showing fallback dialog for version detection failure")
            AlertDialog.Builder(context)
                .setTitle("Unable to Check for Updates")
                .setMessage("We couldn't automatically detect the latest version information. Would you like to check for updates manually on Google Play Store?")
                .setPositiveButton("Open Play Store") { dialog, _ ->
                    Log.d(TAG, "showVersionDetectionFailedDialog: User chose to open Play Store")
                    openPlayStore(context)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Log.d(TAG, "showVersionDetectionFailedDialog: User cancelled")
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }

        private fun showNoUpdateDialog(context: Context) {
            Log.d(TAG, "showNoUpdateDialog: Showing no updates available dialog")
            AlertDialog.Builder(context)
                .setTitle("No Updates Available")
                .setMessage("You already have the latest version of the app!")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        private fun showUpdateCheckErrorDialog(context: Context) {
            Log.d(TAG, "showUpdateCheckErrorDialog: Showing update check error dialog")
            AlertDialog.Builder(context)
                .setTitle("Update Check Failed")
                .setMessage("Unable to check for updates. Please check your internet connection and try again.")
                .setPositiveButton("Retry") { dialog, _ ->
                    Log.d(TAG, "showUpdateCheckErrorDialog: User chose to retry")
                    dialog.dismiss()
                    // Retry the update check
                    checkForUpdatesManually(context)
                }
                .setNegativeButton("Open Play Store") { dialog, _ ->
                    Log.d(TAG, "showUpdateCheckErrorDialog: User chose to open Play Store as fallback")
                    openPlayStore(context)
                    dialog.dismiss()
                }
                .setNeutralButton("Cancel") { dialog, _ ->
                    Log.d(TAG, "showUpdateCheckErrorDialog: User cancelled")
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }

        private fun openPlayStore(context: Context) {
            Log.d(TAG, "openPlayStore: Attempting to open Play Store directly for package: $PACKAGE_NAME")
            try {
                // Method 1: Try to open Play Store app directly with specific package and flags
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
                intent.setPackage("com.android.vending") // Force Play Store app specifically
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
                Log.i(TAG, "openPlayStore: Successfully opened Play Store app directly")
            } catch (e: Exception) {
                Log.w(TAG, "openPlayStore: Direct Play Store app opening failed, trying alternative method", e)
                try {
                    // Method 2: Alternative direct approach
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE_NAME")
                    intent.setPackage("com.android.vending") // Still try to force Play Store
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.i(TAG, "openPlayStore: Successfully opened Play Store with alternative method")
                } catch (e2: Exception) {
                    Log.w(TAG, "openPlayStore: Alternative method failed, falling back to browser", e2)
                    // Method 3: Final fallback to browser (without app chooser)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE_NAME")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Don't set any package here to allow browser to handle
                    context.startActivity(intent)
                    Log.i(TAG, "openPlayStore: Successfully opened Play Store in browser as final fallback")
                }
            }
        }
    }
}