package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentAgeBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AgeFragment : Fragment() {

    private lateinit var binding: FragmentAgeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            //send default dob to UI
            updateAgeCalculation(getDefaultDate())

            // Set current date as default for "Today"
            val currentDate = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            todayTextView.text = dateFormat.format(currentDate)

            dobLayout.setOnClickListener {
                showDatePicker()
            }

            shareBt.setOnClickListener {
                captureAndShareScreenshot()
            }
        }
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.ageResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "age_result_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get a content URI for the file using FileProvider
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Create a share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Age Result"))
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select Date of Birth")
        // Apply the custom theme to the Material Date Picker
        builder.setTheme(R.style.CustomMaterialDatePicker)
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val dateFormat = SimpleDateFormat("ddw MMM yyyy", Locale.getDefault())
            binding.dobTextView.text = dateFormat.format(calendar.time)

            // Here you can add logic to calculate and update the age
            Log.d(TAG, "showDatePicker: time = ${calendar.time}")
            updateAgeCalculation(calendar.time)
        }

        picker.show(parentFragmentManager, picker.toString())

    }

    private fun getDefaultDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(2017, Calendar.FEBRUARY, 2, 5, 30, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time // This returns a Date object with the specified default date
    }

    private fun updateAgeCalculation(dob: Date) {
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance()
        birthDate.time = dob

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        binding.apply {
            // Update UI with calculated age
            tvAge.text = age.toString()

            // Calculate months and days
            var months = today.get(Calendar.MONTH) - birthDate.get(Calendar.MONTH)
            var days = today.get(Calendar.DAY_OF_MONTH) - birthDate.get(Calendar.DAY_OF_MONTH)

            if (days < 0) {
                months--
                days += today.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            if (months < 0) {
                months += 12
            }

            tvAgeDetail.text = "$months months | $days days"

            // Calculate next birthday
            val nextBirthday = Calendar.getInstance()
            nextBirthday.time = birthDate.time
            nextBirthday.set(Calendar.YEAR, today.get(Calendar.YEAR))
            if (nextBirthday.before(today)) {
                nextBirthday.add(Calendar.YEAR, 1)
            }

            val daysUntilBirthday =
                ((nextBirthday.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            val monthsUntilBirthday = daysUntilBirthday / 30
            val remainingDays = daysUntilBirthday % 30

            tvNextBirthday.text =
                SimpleDateFormat("EEEE", Locale.getDefault()).format(nextBirthday.time)
            tvNextBirthdayDetail.text = "$monthsUntilBirthday months | $remainingDays days"

            // Update summary
            tvYears.text = age.toString()
            tvMonths.text = (age * 12 + months).toString()
            tvWeeks.text =
                ((today.timeInMillis - birthDate.timeInMillis) / (7 * 24 * 60 * 60 * 1000)).toString()
            tvDays.text =
                ((today.timeInMillis - birthDate.timeInMillis) / (24 * 60 * 60 * 1000)).toString()
            tvHours.text =
                ((today.timeInMillis - birthDate.timeInMillis) / (60 * 60 * 1000)).toString()
            tvMinutes.text =
                ((today.timeInMillis - birthDate.timeInMillis) / (60 * 1000)).toString()
        }
    }


    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // hide keyboard when clicked outside the keyboard or edit text functionality
    private fun hideKeyboardFunctionality(view: View) {
        // Set up the touch listener for non-text box views
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // Get the currently focused view (e.g., EditText)
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus() // Clear focus to remove cursor from EditText
                }
            }
            false
        }
    }

    private fun animateView(view: View) {
        // Use coroutine to stop the animation after 5 seconds
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            // Start the glow animation and store the animator reference
            glowAnimator = view.startGlowAnimation()
            delay(3000)  // Wait for 5 seconds
            view.stopGlowAnimation(glowAnimator)  // Stop the animation after delay
        }
    }

    companion object {
        private const val TAG = "AgeFragment"
    }
}