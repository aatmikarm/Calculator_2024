package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.AgeHistoryAdapter
import com.aatmik.calculator.databinding.DialogSaveUserBinding
import com.aatmik.calculator.databinding.FragmentAgeBinding
import com.aatmik.calculator.viewmodel.AgeViewModel
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

    private var isDatePickerOpen = false // Flag to track if picker is open
    private lateinit var sharedPreferences: SharedPreferences
    private val ageListKey = "AGE_LIST"

    private lateinit var adapter: AgeHistoryAdapter

    private lateinit var binding: FragmentAgeBinding
    private lateinit var ageViewModel: AgeViewModel // Declare ViewModel variable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAgeBinding.inflate(inflater, container, false)
        // Initialize SharedPreferences here
        sharedPreferences =
            requireActivity().getSharedPreferences("age_history", Context.MODE_PRIVATE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the ViewModel using ViewModelProvider
        ageViewModel = ViewModelProvider(this).get(AgeViewModel::class.java)


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

            // Display saved age history
            displayAgeHistory()

            swipeDelete()
        }
    }

    private fun swipeDelete() {
        // Set up ItemTouchHelper for swipe-to-delete
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return false // We are not implementing drag-and-drop here
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val age = adapter.getItem(position)

                // Remove the item from adapter and shared preferences
                adapter.removeItem(position)
                removeAgeFromHistory(age)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_delete
                    ) // Your delete icon
                    val backgroundColor = ColorDrawable(Color.RED) // Red background

                    // Calculate bounds for the icon and background
                    val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + icon.intrinsicHeight

                    if (dX < 0) { // Swiping left
                        val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        backgroundColor.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                    } else if (dX > 0) { // Swiping right
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                        backgroundColor.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )
                    } else { // No swipe (reset bounds)
                        backgroundColor.setBounds(0, 0, 0, 0)
                    }

                    // Draw the background and icon
                    backgroundColor.draw(c)
                    icon.draw(c)
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

        }

        // Attach the ItemTouchHelper to the RecyclerView
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.ageHistoryRecyclerView)
    }

    private fun removeAgeFromHistory(age: String) {
        val ageHistory = getAgeHistory() // Get the current age history
        if (ageHistory.contains(age)) {
            ageHistory.remove(age) // Remove the specified age from the list
            saveAgeHistory(ageHistory) // Save the updated list back to SharedPreferences
        }
    }

    private fun saveAgeHistory(ageList: List<String>) {
        val updatedAges = ageList.joinToString(",") // Convert list back to comma-separated string
        sharedPreferences.edit().putString(ageListKey, updatedAges).apply() // Save the updated list
    }

    // Step 1: Display Dialog for User Information (Name)
    private fun showUserInfoDialog(dob: Date, age: Int) {
        val dialogBinding = DialogSaveUserBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter User Information")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val userName = dialogBinding.etUserName.text.toString()
                if (userName.isNotEmpty()) {
                    // Save the name, dob, and age to history
                    saveAgeToHistory(userName, dob, age)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun saveAgeToHistory(userName: String, dob: Date, age: Int) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dobString = dateFormat.format(dob)

        // Create the user info string
        val userInfo = "$userName - DOB: $dobString - Age: $age"

        // Fetch the current age list from shared preferences
        val ageList = getAgeHistory().toMutableList()

        // Add the new user info to the list
        ageList.add(userInfo)

        // Save the updated list back to SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString(
            ageListKey,
            ageList.joinToString(",") // Convert list to a comma-separated string
        )
        editor.apply()

        displayAgeHistory()
    }


    // Step 3: Retrieve Age History
    private fun getAgeHistory(): MutableList<String> {
        val savedAges = sharedPreferences.getString(ageListKey, "") ?: ""
        return if (savedAges.isNotEmpty()) {
            savedAges.split(",").toMutableList() // Convert to MutableList
        } else {
            mutableListOf() // Return empty MutableList instead of emptyList()
        }
    }


    private fun displayAgeHistory() {
        val ageList = getAgeHistory() // Ensure this returns a valid list

        if (ageList.isEmpty()) {
            // Show the TextView and hide the RecyclerView
            binding.ageHistoryTextView.visibility = View.VISIBLE
            binding.ageHistoryRecyclerView.visibility = View.GONE
        } else {
            // Hide the TextView and show the RecyclerView
            binding.ageHistoryTextView.visibility = View.GONE
            binding.ageHistoryRecyclerView.visibility = View.VISIBLE

            adapter = AgeHistoryAdapter(ageList) {
                // Handle item click here
                Log.d(TAG, "displayAgeHistory: $it")
                extractDobAndUpdate(it)
            }
            binding.ageHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.ageHistoryRecyclerView.adapter = adapter
        }

        Log.d(TAG, "Age List Size: ${ageList.size}")
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
        if (isDatePickerOpen) return // Prevent opening multiple instances
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select Date of Birth")
        // Apply the custom theme to the Material Date Picker
        builder.setTheme(R.style.CustomMaterialDatePicker)
        val picker = builder.build()

        // Set the flag when the picker is shown
        isDatePickerOpen = true

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.dobTextView.text = dateFormat.format(calendar.time)

            // Calculate and update the age
            Log.d(TAG, "showDatePicker: time = ${calendar.time}")
            updateAgeCalculation(calendar.time)

            // Calculate age
            val today = Calendar.getInstance()
            val birthDate = Calendar.getInstance()
            birthDate.time = calendar.time

            var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            // Show dialog to enter user name
            showUserInfoDialog(calendar.time, age)
        }

        // Reset the flag when the picker is dismissed or a date is selected
        picker.addOnDismissListener {
            isDatePickerOpen = false
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


    private fun extractDobAndUpdate(data: String) {
        // Split the string to get the DOB part
        val dobPart = data.split("-")[1].trim() // "DOB: 11 Apr 1957"

        // Extract the date string
        val dobString = dobPart.removePrefix("DOB: ").trim() // "11 Apr 1957"

        // Parse the date string into a Date object
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dob: Date? = dateFormat.parse(dobString)

        // Check if dob is valid
        if (dob != null) {
            updateAgeCalculation(dob) // Call the function with the Date object
        } else {
            // Handle invalid date parsing if needed
            Log.e("AgeHistory", "Invalid date format: $dobString")
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