package com.aatmik.calculator.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.databinding.RatingDialogLayoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RatingManager {

    companion object {
        private const val PREFS_NAME = "rating_prefs"
        private const val KEY_USED_CALCULATORS = "used_calculators"
        private const val KEY_RATING_SHOWN = "rating_shown"
        private const val KEY_NEVER_SHOW_AGAIN = "never_show_again"
        private const val USAGE_THRESHOLD = 10

        fun trackCalculatorUsage(context: Context, calculatorName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Check if user has already rated or chosen never to show again
            if (prefs.getBoolean(KEY_RATING_SHOWN, false) ||
                prefs.getBoolean(KEY_NEVER_SHOW_AGAIN, false)) {
                android.util.Log.d("RatingManager", "Dialog blocked - already shown or never show")
                return
            }

            // Get the set of used calculators
            val usedCalculators = prefs.getStringSet(KEY_USED_CALCULATORS, mutableSetOf()) ?: mutableSetOf()
            val newUsedCalculators = usedCalculators.toMutableSet()

            // Add the current calculator if it's not already in the set
            if (!newUsedCalculators.contains(calculatorName)) {
                newUsedCalculators.add(calculatorName)

                // Save the updated set
                prefs.edit()
                    .putStringSet(KEY_USED_CALCULATORS, newUsedCalculators)
                    .apply()

                android.util.Log.d("RatingManager", "Calculator tracked: $calculatorName | Count: ${newUsedCalculators.size}/$USAGE_THRESHOLD")

                // Check if we've reached the threshold
                if (newUsedCalculators.size >= USAGE_THRESHOLD) {
                    android.util.Log.d("RatingManager", "Threshold reached! Showing rating dialog")
                    showRatingDialog(context)
                }
            } else {
                android.util.Log.d("RatingManager", "Calculator already tracked: $calculatorName")
            }
        }

        private fun showRatingDialog(context: Context) {
            if (context !is AppCompatActivity) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Ensure dialog is shown on main thread
            context.runOnUiThread {
                // Option 1: Simple AlertDialog (easier to implement)
                // showSimpleRatingDialog(context, prefs)

                // Option 2: Custom Dialog with ViewBinding (uncomment if you want to use the custom layout)
                showCustomRatingDialog(context, prefs)
            }
        }

        private fun showSimpleRatingDialog(context: AppCompatActivity, prefs: android.content.SharedPreferences) {
            AlertDialog.Builder(context)
                .setTitle("Rate Our App")
                .setMessage("We hope you're enjoying our calculator app! Would you mind taking a moment to rate us on Google Play Store? Your feedback helps us improve.")
                .setPositiveButton("Rate Now") { dialog, _ ->
                    // Mark as shown and rated
                    prefs.edit()
                        .putBoolean(KEY_RATING_SHOWN, true)
                        .apply()

                    // Open Play Store
                    openPlayStore(context)
                    dialog.dismiss()
                }
                .setNegativeButton("Maybe Later") { dialog, _ ->
                    // Reset the counter so it can show again after more usage
                    prefs.edit()
                        .remove(KEY_USED_CALCULATORS)
                        .apply()
                    dialog.dismiss()
                }
                .setNeutralButton("Never") { dialog, _ ->
                    // Mark to never show again
                    prefs.edit()
                        .putBoolean(KEY_NEVER_SHOW_AGAIN, true)
                        .apply()
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }

        private fun showCustomRatingDialog(context: AppCompatActivity, prefs: android.content.SharedPreferences) {
            val binding = RatingDialogLayoutBinding.inflate(LayoutInflater.from(context))

            val dialog = MaterialAlertDialogBuilder(context)
                .setView(binding.root)
                .setCancelable(false)
                .create()

            binding.btnClose.setOnClickListener { dialog.dismiss() }

            binding.btnNever.setOnClickListener {
                prefs.edit().putBoolean(KEY_NEVER_SHOW_AGAIN, true).apply()
                dialog.dismiss()
            }

            binding.btnLater.setOnClickListener {
                prefs.edit().remove(KEY_USED_CALCULATORS).apply()
                dialog.dismiss()
            }

            binding.btnRate.setOnClickListener {
                prefs.edit().putBoolean(KEY_RATING_SHOWN, true).apply()
                openPlayStore(context)
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun openPlayStore(context: Context) {
            try {
                val appPackageName = "com.aatmik.calculator"
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName&showRating=true")
                )
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to browser if Play Store app is not available
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.aatmik.calculator")
                )
                context.startActivity(intent)
            }
        }
    }
}