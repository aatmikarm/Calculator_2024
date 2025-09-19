package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.PregnancyHistoryAdapter
import com.aatmik.calculator.databinding.DialogSavePregnancyBinding
import com.aatmik.calculator.databinding.FragmentPregnancyCalculatorBinding
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

class PregnancyCalculatorFragment : Fragment() {

    private var isDatePickerOpen = false
    private lateinit var sharedPreferences: SharedPreferences
    private val pregnancyListKey = "PREGNANCY_LIST"
    private lateinit var adapter: PregnancyHistoryAdapter
    private lateinit var binding: FragmentPregnancyCalculatorBinding
    private var isCalculating = false

    // Pregnancy calculation constants
    companion object {
        private const val TAG = "PregnancyCalculatorFragment"
        private const val PREGNANCY_DURATION_DAYS = 280 // 40 weeks
        private const val DAYS_IN_WEEK = 7
        private val WEEKS_IN_TRIMESTER = arrayOf(0, 13, 27, 40)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPregnancyCalculatorBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("pregnancy_history", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        animateView(binding.pregnancyLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()
        setupListeners()
        setupTextWatchers()

        // Set default calculation method to LMP
        updatePregnancyCalculation(getDefaultLMPDate(), "LMP")

        displayPregnancyHistory()
        setupSwipeDelete()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Calculation Method Spinner
            val calculationMethods = arrayOf("Last Menstrual Period (LMP)", "Conception Date", "Due Date")
            val methodAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                calculationMethods
            )
            methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCalculationMethod.adapter = methodAdapter
            spinnerCalculationMethod.setSelection(0) // Default to LMP
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            shareBt.setOnClickListener {
                captureAndShareScreenshot()
            }

            btnReset.setOnClickListener {
                resetCalculation()
            }

            btnSave.setOnClickListener {
                showSaveDialog()
            }

            // Date selection listeners
            lmpLayout.setOnClickListener {
                showDatePicker("LMP")
            }

            conceptionLayout.setOnClickListener {
                showDatePicker("Conception")
            }

            dueDateLayout.setOnClickListener {
                showDatePicker("Due Date")
            }

            // Spinner listener
            spinnerCalculationMethod.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateInputVisibility(position)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Set current date as default for "Today"
            val currentDate = Calendar.getInstance().time
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            todayTextView.text = dateFormat.format(currentDate)
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    recalculateBasedOnMethod()
                }
            }
        }

        binding.apply {
            etBabyName.addTextChangedListener(textWatcher)
            etCycleLength.addTextChangedListener(textWatcher)
        }
    }

    private fun updateInputVisibility(methodIndex: Int) {
        binding.apply {
            // Hide all layouts first
            lmpLayout.visibility = View.GONE
            conceptionLayout.visibility = View.GONE
            dueDateLayout.visibility = View.GONE

            // Show relevant layout based on selection
            when (methodIndex) {
                0 -> { // LMP
                    lmpLayout.visibility = View.VISIBLE
                    tvCalculationMethodTitle.text = "Last Menstrual Period"
                }
                1 -> { // Conception
                    conceptionLayout.visibility = View.VISIBLE
                    tvCalculationMethodTitle.text = "Conception Date"
                }
                2 -> { // Due Date
                    dueDateLayout.visibility = View.VISIBLE
                    tvCalculationMethodTitle.text = "Expected Due Date"
                }
            }
        }
    }

    private fun showDatePicker(dateType: String) {
        if (isDatePickerOpen) return

        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select $dateType")
        builder.setTheme(R.style.CustomMaterialDatePicker)
        val picker = builder.build()

        isDatePickerOpen = true

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            when (dateType) {
                "LMP" -> {
                    binding.lmpTextView.text = dateFormat.format(calendar.time)
                    updatePregnancyCalculation(calendar.time, "LMP")
                }
                "Conception" -> {
                    binding.conceptionTextView.text = dateFormat.format(calendar.time)
                    updatePregnancyCalculation(calendar.time, "Conception")
                }
                "Due Date" -> {
                    binding.dueDateTextView.text = dateFormat.format(calendar.time)
                    updatePregnancyCalculation(calendar.time, "DueDate")
                }
            }
        }

        picker.addOnDismissListener {
            isDatePickerOpen = false
        }

        picker.show(parentFragmentManager, picker.toString())
    }

    private fun getDefaultLMPDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -20) // 20 weeks ago
        return calendar.time
    }

    private fun updatePregnancyCalculation(inputDate: Date, calculationType: String) {
        val today = Calendar.getInstance()
        val inputCalendar = Calendar.getInstance()
        inputCalendar.time = inputDate

        val lmpDate: Calendar
        val dueDate: Calendar
        val conceptionDate: Calendar

        when (calculationType) {
            "LMP" -> {
                lmpDate = inputCalendar
                conceptionDate = Calendar.getInstance().apply {
                    time = lmpDate.time
                    add(Calendar.DAY_OF_YEAR, 14) // Conception typically occurs 14 days after LMP
                }
                dueDate = Calendar.getInstance().apply {
                    time = lmpDate.time
                    add(Calendar.DAY_OF_YEAR, PREGNANCY_DURATION_DAYS)
                }
            }
            "Conception" -> {
                conceptionDate = inputCalendar
                lmpDate = Calendar.getInstance().apply {
                    time = conceptionDate.time
                    add(Calendar.DAY_OF_YEAR, -14)
                }
                dueDate = Calendar.getInstance().apply {
                    time = conceptionDate.time
                    add(Calendar.DAY_OF_YEAR, PREGNANCY_DURATION_DAYS - 14)
                }
            }
            "DueDate" -> {
                dueDate = inputCalendar
                lmpDate = Calendar.getInstance().apply {
                    time = dueDate.time
                    add(Calendar.DAY_OF_YEAR, -PREGNANCY_DURATION_DAYS)
                }
                conceptionDate = Calendar.getInstance().apply {
                    time = lmpDate.time
                    add(Calendar.DAY_OF_YEAR, 14)
                }
            }
            else -> return
        }

        // Calculate current pregnancy progress
        val daysSinceLMP = ((today.timeInMillis - lmpDate.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        val weeksSinceLMP = daysSinceLMP / DAYS_IN_WEEK
        val daysPastWeek = daysSinceLMP % DAYS_IN_WEEK

        binding.apply {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            // Update all date displays
            lmpTextView.text = dateFormat.format(lmpDate.time)
            conceptionTextView.text = dateFormat.format(conceptionDate.time)
            estimatedDueDateTextView.text = dateFormat.format(dueDate.time)

            // Update pregnancy progress
            if (daysSinceLMP >= 0) {
                tvCurrentWeek.text = weeksSinceLMP.toString()
                tvCurrentDay.text = daysPastWeek.toString()
                tvPregnancyDetail.text = "${weeksSinceLMP}w ${daysPastWeek}d"

                // Calculate trimester
                val trimester = when {
                    weeksSinceLMP < WEEKS_IN_TRIMESTER[1] -> 1
                    weeksSinceLMP < WEEKS_IN_TRIMESTER[2] -> 2
                    weeksSinceLMP < WEEKS_IN_TRIMESTER[3] -> 3
                    else -> 3 // Post-term
                }

                tvTrimester.text = when (trimester) {
                    1 -> "First Trimester"
                    2 -> "Second Trimester"
                    3 -> "Third Trimester"
                    else -> "Post-term"
                }

                // Days until due date
                val daysUntilDue = ((dueDate.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                val weeksUntilDue = daysUntilDue / 7
                val remainingDays = daysUntilDue % 7

                if (daysUntilDue > 0) {
                    tvDaysUntilDue.text = daysUntilDue.toString()
                    tvDueDetail.text = "${weeksUntilDue}w ${remainingDays}d remaining"
                    tvPregnancyStatus.text = "Pregnancy in progress"
                } else if (daysUntilDue == 0) {
                    tvDaysUntilDue.text = "0"
                    tvDueDetail.text = "Due today!"
                    tvPregnancyStatus.text = "Baby due today!"
                } else {
                    tvDaysUntilDue.text = Math.abs(daysUntilDue).toString()
                    tvDueDetail.text = "${Math.abs(daysUntilDue)} days overdue"
                    tvPregnancyStatus.text = "Post-term pregnancy"
                }

                // Update summary statistics
                updateSummaryStats(lmpDate, today, dueDate, weeksSinceLMP, daysSinceLMP)

                // Update detailed information
                updateDetailedInfo(weeksSinceLMP, trimester, daysUntilDue)

            } else {
                // Future pregnancy
                tvPregnancyStatus.text = "Pregnancy starts in ${Math.abs(daysSinceLMP)} days"
                tvCurrentWeek.text = "0"
                tvCurrentDay.text = "0"
            }
        }
    }

    private fun updateSummaryStats(lmpDate: Calendar, today: Calendar, dueDate: Calendar, weeks: Int, days: Int) {
        binding.apply {
            tvWeeksPregnant.text = weeks.toString()
            tvDaysPregnant.text = days.toString()
            tvMonthsPregnant.text = (weeks / 4).toString()

            // Calculate hours and minutes
            val hoursSinceLMP = (days * 24).toString()
            val minutesSinceLMP = (days * 24 * 60).toString()

            tvHoursPregnant.text = hoursSinceLMP
            tvMinutesPregnant.text = minutesSinceLMP
        }
    }

    private fun updateDetailedInfo(weeks: Int, trimester: Int, daysUntilDue: Int) {
        binding.apply {
            val info = StringBuilder()
            info.append("Pregnancy Information\n\n")

            // Current stage info
            info.append("Current Stage: $weeks weeks pregnant\n")
            info.append("Trimester: ${getTrimesterName(trimester)}\n\n")

            // Baby development info
            info.append("Baby Development:\n")
            info.append(getBabyDevelopmentInfo(weeks))
            info.append("\n\n")

            // What to expect
            info.append("What to Expect:\n")
            info.append(getExpectationInfo(weeks, trimester))
            info.append("\n\n")

            // Important milestones
            info.append("Important Milestones:\n")
            info.append(getMilestoneInfo(weeks))

            tvPregnancyDetails.text = info.toString()
        }
    }

    private fun getTrimesterName(trimester: Int): String {
        return when (trimester) {
            1 -> "First Trimester (Weeks 1-12)"
            2 -> "Second Trimester (Weeks 13-26)"
            3 -> "Third Trimester (Weeks 27-40)"
            else -> "Post-term"
        }
    }

    private fun getBabyDevelopmentInfo(weeks: Int): String {
        return when {
            weeks <= 4 -> "Embryo is developing basic organs and neural tube"
            weeks <= 8 -> "Baby's heart is beating, limbs are forming"
            weeks <= 12 -> "Baby's organs are developed, gender may be visible"
            weeks <= 16 -> "Baby can hear sounds, hair and nails growing"
            weeks <= 20 -> "Baby's movements can be felt, bones are hardening"
            weeks <= 24 -> "Baby's brain is developing rapidly, viable outside womb"
            weeks <= 28 -> "Baby's eyes can open, lungs developing"
            weeks <= 32 -> "Baby gaining weight, bones hardening except skull"
            weeks <= 36 -> "Baby is considered full-term soon, organs maturing"
            weeks <= 40 -> "Baby is ready for birth, head may engage in pelvis"
            else -> "Baby is post-term, monitoring is important"
        }
    }

    private fun getExpectationInfo(weeks: Int, trimester: Int): String {
        return when (trimester) {
            1 -> "Morning sickness, fatigue, breast changes, frequent urination"
            2 -> "Energy returns, baby bump shows, feel baby movements"
            3 -> "Shortness of breath, back pain, frequent urination, nesting instinct"
            else -> "Regular monitoring, prepare for labor"
        }
    }

    private fun getMilestoneInfo(weeks: Int): String {
        val milestones = mutableListOf<String>()

        if (weeks >= 12) milestones.add("End of first trimester (Week 12)")
        if (weeks >= 20) milestones.add("Anatomy scan (Weeks 18-20)")
        if (weeks >= 24) milestones.add("Viability milestone (Week 24)")
        if (weeks >= 28) milestones.add("Third trimester begins (Week 28)")
        if (weeks >= 32) milestones.add("Important brain development (Week 32)")
        if (weeks >= 37) milestones.add("Full-term pregnancy (Week 37)")
        if (weeks >= 40) milestones.add("Due date reached (Week 40)")

        return if (milestones.isEmpty()) {
            "Early pregnancy development phase"
        } else {
            milestones.joinToString("\n")
        }
    }

    private fun showSaveDialog() {
        val dialogBinding = DialogSavePregnancyBinding.inflate(layoutInflater)

        // Pre-fill with current baby name if available
        dialogBinding.etMotherName.setText(binding.etBabyName.text.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Save Pregnancy Information")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val motherName = dialogBinding.etMotherName.text.toString()
                val babyName = dialogBinding.etBabyName.text.toString()
                if (motherName.isNotEmpty()) {
                    savePregnancyToHistory(motherName, babyName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun savePregnancyToHistory(motherName: String, babyName: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val lmpDate = binding.lmpTextView.text.toString()
        val dueDate = binding.estimatedDueDateTextView.text.toString()
        val currentWeek = binding.tvCurrentWeek.text.toString()

        val pregnancyInfo = "$motherName - Baby: ${babyName.ifEmpty { "Not specified" }} - Week: $currentWeek - Due: $dueDate"

        val pregnancyList = getPregnancyHistory().toMutableList()
        pregnancyList.add(pregnancyInfo)

        val editor = sharedPreferences.edit()
        editor.putString(pregnancyListKey, pregnancyList.joinToString(","))
        editor.apply()

        displayPregnancyHistory()
    }

    private fun getPregnancyHistory(): MutableList<String> {
        val savedPregnancies = sharedPreferences.getString(pregnancyListKey, "") ?: ""
        return if (savedPregnancies.isNotEmpty()) {
            savedPregnancies.split(",").toMutableList()
        } else {
            mutableListOf()
        }
    }

    private fun displayPregnancyHistory() {
        val pregnancyList = getPregnancyHistory()

        if (pregnancyList.isEmpty()) {
            binding.pregnancyHistoryTextView.visibility = View.VISIBLE
            binding.pregnancyHistoryRecyclerView.visibility = View.GONE
        } else {
            binding.pregnancyHistoryTextView.visibility = View.GONE
            binding.pregnancyHistoryRecyclerView.visibility = View.VISIBLE

            adapter = PregnancyHistoryAdapter(pregnancyList) { pregnancy ->
                Log.d(TAG, "Selected pregnancy: $pregnancy")
                // Handle pregnancy history item click
            }
            binding.pregnancyHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.pregnancyHistoryRecyclerView.adapter = adapter
        }
    }

    private fun setupSwipeDelete() {
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val pregnancy = adapter.getItem(position)
                adapter.removeItem(position)
                removePregnancyFromHistory(pregnancy)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.pregnancyHistoryRecyclerView)
    }

    private fun removePregnancyFromHistory(pregnancy: String) {
        val pregnancyHistory = getPregnancyHistory()
        if (pregnancyHistory.contains(pregnancy)) {
            pregnancyHistory.remove(pregnancy)
            savePregnancyHistory(pregnancyHistory)
        }
    }

    private fun savePregnancyHistory(pregnancyList: List<String>) {
        val updatedPregnancies = pregnancyList.joinToString(",")
        sharedPreferences.edit().putString(pregnancyListKey, updatedPregnancies).apply()
    }

    private fun resetCalculation() {
        binding.apply {
            etBabyName.text?.clear()
            etCycleLength.setText("28")

            // Reset to default LMP date
            updatePregnancyCalculation(getDefaultLMPDate(), "LMP")
        }
    }

    private fun recalculateBasedOnMethod() {
        val selectedMethod = binding.spinnerCalculationMethod.selectedItemPosition
        when (selectedMethod) {
            0 -> { // LMP
                val lmpText = binding.lmpTextView.text.toString()
                if (lmpText != "Select LMP Date") {
                    try {
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val lmpDate = dateFormat.parse(lmpText)
                        if (lmpDate != null) {
                            updatePregnancyCalculation(lmpDate, "LMP")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing LMP date", e)
                    }
                }
            }
            // Add similar logic for other methods
        }
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.pregnancyResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "pregnancy_result_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Pregnancy Information"))
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun hideKeyboardFunctionality(view: View) {
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus()
                }
            }
            false
        }
    }

    private fun animateView(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            glowAnimator = view.startGlowAnimation()
            delay(3000)
            view.stopGlowAnimation(glowAnimator)
        }
    }
}