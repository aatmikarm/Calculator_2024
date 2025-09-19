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
import com.aatmik.calculator.adapter.FertilityHistoryAdapter
import com.aatmik.calculator.databinding.DialogSaveFertilityBinding
import com.aatmik.calculator.databinding.FragmentOvulationFertilityBinding
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
import kotlin.math.roundToInt

class OvulationFertilityFragment : Fragment() {

    private var isDatePickerOpen = false
    private lateinit var sharedPreferences: SharedPreferences
    private val fertilityListKey = "FERTILITY_LIST"
    private lateinit var adapter: FertilityHistoryAdapter
    private lateinit var binding: FragmentOvulationFertilityBinding
    private var isCalculating = false

    companion object {
        private const val TAG = "OvulationFertilityFragment"
        private const val DEFAULT_CYCLE_LENGTH = 28
        private const val DEFAULT_LUTEAL_PHASE_LENGTH = 14
        private const val SPERM_SURVIVAL_DAYS = 5
        private const val EGG_SURVIVAL_HOURS = 24
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOvulationFertilityBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("fertility_history", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        animateView(binding.fertilityLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()
        setupListeners()
        setupTextWatchers()

        setDefaultValues()
        updateFertilityCalculation()

        displayFertilityHistory()
        setupSwipeDelete()
    }

    private fun setDefaultValues() {
        binding.apply {
            etCycleLength.setText(DEFAULT_CYCLE_LENGTH.toString())
            etLutealPhaseLength.setText(DEFAULT_LUTEAL_PHASE_LENGTH.toString())

            // Set default last period date to 14 days ago (mid-cycle)
            val defaultDate = Calendar.getInstance()
            defaultDate.add(Calendar.DAY_OF_YEAR, -14)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            lastPeriodTextView.text = dateFormat.format(defaultDate.time)

            // Set current date
            val currentDate = Calendar.getInstance().time
            todayTextView.text = dateFormat.format(currentDate)
        }
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Goal Spinner
            val goals = arrayOf("Trying to Conceive", "Avoiding Pregnancy", "Cycle Tracking")
            val goalAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                goals
            )
            goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGoal.adapter = goalAdapter
            spinnerGoal.setSelection(0) // Default to trying to conceive

            // Setup Cervical Mucus Spinner
            val mucusTypes = arrayOf("Dry", "Sticky", "Creamy", "Egg White", "Not Tracked")
            val mucusAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                mucusTypes
            )
            mucusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCervicalMucus.adapter = mucusAdapter
            spinnerCervicalMucus.setSelection(4) // Default to not tracked
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

            btnLogToday.setOnClickListener {
                logTodayData()
            }

            // Date selection listeners
            lastPeriodLayout.setOnClickListener {
                showDatePicker("Last Period")
            }

            // Spinner listeners
            spinnerGoal.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateFertilityCalculation()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerCervicalMucus.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateFertilityCalculation()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    updateFertilityCalculation()
                }
            }
        }

        binding.apply {
            etCycleLength.addTextChangedListener(textWatcher)
            etLutealPhaseLength.addTextChangedListener(textWatcher)
            etBasalBodyTemp.addTextChangedListener(textWatcher)
            etUserName.addTextChangedListener(textWatcher)
        }
    }

    private fun showDatePicker(dateType: String) {
        if (isDatePickerOpen) return

        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Select $dateType Date")
        builder.setTheme(R.style.CustomMaterialDatePicker)
        val picker = builder.build()

        isDatePickerOpen = true

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            when (dateType) {
                "Last Period" -> {
                    binding.lastPeriodTextView.text = dateFormat.format(calendar.time)
                    updateFertilityCalculation()
                }
            }
        }

        picker.addOnDismissListener {
            isDatePickerOpen = false
        }

        picker.show(parentFragmentManager, picker.toString())
    }

    private fun updateFertilityCalculation() {
        if (isCalculating) return
        isCalculating = true

        try {
            binding.apply {
                val cycleLength = etCycleLength.text.toString().toIntOrNull() ?: DEFAULT_CYCLE_LENGTH
                val lutealPhaseLength = etLutealPhaseLength.text.toString().toIntOrNull() ?: DEFAULT_LUTEAL_PHASE_LENGTH
                val basalBodyTemp = etBasalBodyTemp.text.toString().toDoubleOrNull() ?: 0.0
                val cervicalMucus = spinnerCervicalMucus.selectedItem.toString()
                val goal = spinnerGoal.selectedItem.toString()

                // Parse last period date
                val lastPeriodText = lastPeriodTextView.text.toString()
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val lastPeriodDate = try {
                    dateFormat.parse(lastPeriodText) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                val today = Calendar.getInstance()
                val lastPeriod = Calendar.getInstance().apply { time = lastPeriodDate }

                // Calculate ovulation date (luteal phase length before next period)
                val nextPeriod = Calendar.getInstance().apply {
                    time = lastPeriodDate
                    add(Calendar.DAY_OF_YEAR, cycleLength)
                }

                val ovulationDate = Calendar.getInstance().apply {
                    time = nextPeriod.time
                    add(Calendar.DAY_OF_YEAR, -lutealPhaseLength)
                }

                // Calculate fertile window (5 days before ovulation + ovulation day)
                val fertileStart = Calendar.getInstance().apply {
                    time = ovulationDate.time
                    add(Calendar.DAY_OF_YEAR, -SPERM_SURVIVAL_DAYS)
                }

                val fertileEnd = Calendar.getInstance().apply {
                    time = ovulationDate.time
                    add(Calendar.DAY_OF_YEAR, 1)
                }

                // Days since last period
                val daysSinceLastPeriod = ((today.timeInMillis - lastPeriod.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

                // Days until ovulation
                val daysUntilOvulation = ((ovulationDate.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

                // Calculate current fertility probability
                val fertilityProbability = calculateFertilityProbability(today, fertileStart, fertileEnd, ovulationDate, cervicalMucus, basalBodyTemp)
                val fertilityStatus = getFertilityStatus(fertilityProbability, goal)

                // Update UI
                tvDaysSinceLastPeriod.text = daysSinceLastPeriod.toString()
                tvDaysUntilOvulation.text = if (daysUntilOvulation >= 0) daysUntilOvulation.toString() else "Passed"

                tvFertilityProbability.text = "${fertilityProbability.roundToInt()}%"
                tvFertilityStatus.text = fertilityStatus.status
                tvFertilityStatus.setTextColor(resources.getColor(fertilityStatus.color, null))

                tvFertilityAdvice.text = fertilityStatus.advice

                // Update dates
                tvOvulationDate.text = dateFormat.format(ovulationDate.time)
                tvFertileWindowStart.text = dateFormat.format(fertileStart.time)
                tvFertileWindowEnd.text = dateFormat.format(fertileEnd.time)
                tvNextPeriodDate.text = dateFormat.format(nextPeriod.time)

                // Update cycle phase
                val currentPhase = getCurrentCyclePhase(daysSinceLastPeriod, cycleLength, lutealPhaseLength)
                tvCurrentPhase.text = currentPhase.name
                tvPhaseDescription.text = currentPhase.description

                // Update summary statistics
                updateSummaryStats(cycleLength, lutealPhaseLength, daysSinceLastPeriod, fertilityProbability)

                // Update detailed information
                updateDetailedInfo(currentPhase, fertilityProbability, goal, cervicalMucus, basalBodyTemp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating fertility information", e)
        } finally {
            isCalculating = false
        }
    }

    private fun calculateFertilityProbability(
        today: Calendar,
        fertileStart: Calendar,
        fertileEnd: Calendar,
        ovulationDate: Calendar,
        cervicalMucus: String,
        basalBodyTemp: Double
    ): Double {
        val todayTime = today.timeInMillis
        val fertileStartTime = fertileStart.timeInMillis
        val fertileEndTime = fertileEnd.timeInMillis
        val ovulationTime = ovulationDate.timeInMillis

        var baseProbability = 0.0

        // Base probability based on cycle day
        when {
            todayTime < fertileStartTime -> baseProbability = 1.0
            todayTime > fertileEndTime -> baseProbability = 1.0
            todayTime == ovulationTime -> baseProbability = 35.0
            todayTime == ovulationTime - (24 * 60 * 60 * 1000) -> baseProbability = 30.0 // Day before ovulation
            todayTime in fertileStartTime..fertileEndTime -> {
                val daysFromOvulation = ((todayTime - ovulationTime) / (24 * 60 * 60 * 1000)).toInt()
                baseProbability = when (kotlin.math.abs(daysFromOvulation)) {
                    0 -> 35.0
                    1 -> 25.0
                    2 -> 15.0
                    3 -> 10.0
                    4 -> 8.0
                    5 -> 5.0
                    else -> 2.0
                }
            }
        }

        // Adjust based on cervical mucus
        val mucusMultiplier = when (cervicalMucus) {
            "Egg White" -> 1.3
            "Creamy" -> 1.1
            "Sticky" -> 0.8
            "Dry" -> 0.6
            else -> 1.0
        }

        // Adjust based on basal body temperature (simplified)
        val tempMultiplier = when {
            basalBodyTemp > 0 && basalBodyTemp < 97.8 -> 1.2 // Pre-ovulation temp
            basalBodyTemp >= 98.0 -> 0.7 // Post-ovulation temp
            else -> 1.0
        }

        return (baseProbability * mucusMultiplier * tempMultiplier).coerceIn(0.0, 40.0)
    }

    private fun getFertilityStatus(probability: Double, goal: String): FertilityStatus {
        return when {
            probability >= 25 -> FertilityStatus(
                "Peak Fertility",
                if (goal == "Trying to Conceive") "Optimal time for conception - have intercourse today and tomorrow"
                else "High pregnancy risk - use protection or avoid intercourse",
                android.R.color.holo_orange_dark
            )
            probability >= 15 -> FertilityStatus(
                "High Fertility",
                if (goal == "Trying to Conceive") "Good time for conception - consider having intercourse"
                else "Moderate pregnancy risk - be cautious",
                android.R.color.holo_orange_light
            )
            probability >= 5 -> FertilityStatus(
                "Moderate Fertility",
                if (goal == "Trying to Conceive") "Lower chance but still possible - continue tracking"
                else "Low pregnancy risk",
                android.R.color.holo_blue_dark
            )
            else -> FertilityStatus(
                "Low Fertility",
                if (goal == "Trying to Conceive") "Very low chance of conception today"
                else "Very low pregnancy risk",
                android.R.color.holo_green_dark
            )
        }
    }

    private fun getCurrentCyclePhase(daysSinceLast: Int, cycleLength: Int, lutealLength: Int): CyclePhase {
        val ovulationDay = cycleLength - lutealLength

        return when {
            daysSinceLast <= 5 -> CyclePhase.MENSTRUAL
            daysSinceLast < ovulationDay - 2 -> CyclePhase.FOLLICULAR
            daysSinceLast in (ovulationDay - 2)..(ovulationDay + 1) -> CyclePhase.OVULATION
            daysSinceLast > ovulationDay + 1 -> CyclePhase.LUTEAL
            else -> CyclePhase.FOLLICULAR
        }
    }

    private fun updateSummaryStats(cycleLength: Int, lutealLength: Int, daysSinceLast: Int, fertilityProb: Double) {
        binding.apply {
            tvCycleLengthSummary.text = "$cycleLength days"
            tvLutealPhaseSummary.text = "$lutealLength days"
            tvCurrentDaySummary.text = "Day $daysSinceLast"

            val ovulationDay = cycleLength - lutealLength
            tvOvulationDaySummary.text = "Day $ovulationDay"

            val fertileWindow = "${ovulationDay - 5}-${ovulationDay + 1}"
            tvFertileWindowSummary.text = "Days $fertileWindow"

            tvTodayFertilitySummary.text = "${fertilityProb.roundToInt()}%"
        }
    }

    private fun updateDetailedInfo(
        phase: CyclePhase,
        fertilityProb: Double,
        goal: String,
        cervicalMucus: String,
        basalBodyTemp: Double
    ) {
        binding.apply {
            val info = StringBuilder()
            info.append("Ovulation & Fertility Analysis\n\n")

            // Current status
            info.append("Current Status:\n")
            info.append("• Cycle Phase: ${phase.name}\n")
            info.append("• Fertility Probability: ${fertilityProb.roundToInt()}%\n")
            info.append("• Goal: $goal\n\n")

            // Fertility signs
            info.append("Fertility Signs:\n")
            info.append("• Cervical Mucus: $cervicalMucus\n")
            if (basalBodyTemp > 0) {
                info.append("• Basal Body Temperature: ${String.format("%.1f", basalBodyTemp)}°F\n")
            }
            info.append("\n")

            // Phase information
            info.append("Current Phase Information:\n")
            info.append("${phase.description}\n\n")

            // Conception information
            info.append("Conception Information:\n")
            info.append("• Sperm can survive up to 5 days in reproductive tract\n")
            info.append("• Egg survives 12-24 hours after ovulation\n")
            info.append("• Peak fertility occurs 2 days before ovulation\n")
            info.append("• Conception most likely on ovulation day and day before\n\n")

            // Tracking tips
            info.append("Tracking Tips:\n")
            info.append("• Monitor cervical mucus daily\n")
            info.append("• Take basal body temperature before getting up\n")
            info.append("• Track consistently for accurate predictions\n")
            info.append("• Note any symptoms or changes\n\n")

            // Goal-specific advice
            if (goal == "Trying to Conceive") {
                info.append("Conception Tips:\n")
                info.append("• Have intercourse every other day during fertile window\n")
                info.append("• Don't stress about timing - consistency matters more\n")
                info.append("• Consider prenatal vitamins with folic acid\n")
                info.append("• Maintain healthy lifestyle and manage stress\n")
            } else if (goal == "Avoiding Pregnancy") {
                info.append("Natural Family Planning:\n")
                info.append("• Avoid unprotected intercourse during fertile window\n")
                info.append("• Use barrier methods during moderate-high fertility days\n")
                info.append("• This method requires consistent tracking\n")
                info.append("• Consider backup contraception methods\n")
            }

            info.append("\n")

            // Disclaimer
            info.append("Important: This calculator provides estimates based on typical cycles. ")
            info.append("Individual fertility can vary significantly. For family planning or contraception, ")
            info.append("consult with a healthcare professional or fertility specialist.")

            tvFertilityDetails.text = info.toString()
        }
    }

    private fun logTodayData() {
        // This could open a dialog to log today's fertility signs
        // For now, just update the calculation
        updateFertilityCalculation()
        showSaveDialog()
    }

    private fun showSaveDialog() {
        val dialogBinding = DialogSaveFertilityBinding.inflate(layoutInflater)

        dialogBinding.etUserName.setText(binding.etUserName.text.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Save Fertility Information")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val userName = dialogBinding.etUserName.text.toString()
                if (userName.isNotEmpty()) {
                    saveFertilityToHistory(userName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun saveFertilityToHistory(userName: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val lastPeriodDate = binding.lastPeriodTextView.text.toString()
        val ovulationDate = binding.tvOvulationDate.text.toString()
        val fertilityProb = binding.tvFertilityProbability.text.toString()
        val currentPhase = binding.tvCurrentPhase.text.toString()
        val goal = binding.spinnerGoal.selectedItem.toString()

        val fertilityInfo = "$userName - Period: $lastPeriodDate - Ovulation: $ovulationDate - Fertility: $fertilityProb - Phase: $currentPhase - Goal: $goal"

        val fertilityList = getFertilityHistory().toMutableList()
        fertilityList.add(fertilityInfo)

        val editor = sharedPreferences.edit()
        editor.putString(fertilityListKey, fertilityList.joinToString(","))
        editor.apply()

        displayFertilityHistory()
    }

    private fun getFertilityHistory(): MutableList<String> {
        val savedFertility = sharedPreferences.getString(fertilityListKey, "") ?: ""
        return if (savedFertility.isNotEmpty()) {
            savedFertility.split(",").toMutableList()
        } else {
            mutableListOf()
        }
    }

    private fun displayFertilityHistory() {
        val fertilityList = getFertilityHistory()

        if (fertilityList.isEmpty()) {
            binding.fertilityHistoryTextView.visibility = View.VISIBLE
            binding.fertilityHistoryRecyclerView.visibility = View.GONE
        } else {
            binding.fertilityHistoryTextView.visibility = View.GONE
            binding.fertilityHistoryRecyclerView.visibility = View.VISIBLE

            adapter = FertilityHistoryAdapter(fertilityList) { fertility ->
                Log.d(TAG, "Selected fertility: $fertility")
            }
            binding.fertilityHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.fertilityHistoryRecyclerView.adapter = adapter
        }
    }

    private fun setupSwipeDelete() {
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val fertility = adapter.getItem(position)
                adapter.removeItem(position)
                removeFertilityFromHistory(fertility)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.fertilityHistoryRecyclerView)
    }

    private fun removeFertilityFromHistory(fertility: String) {
        val fertilityHistory = getFertilityHistory()
        if (fertilityHistory.contains(fertility)) {
            fertilityHistory.remove(fertility)
            saveFertilityHistory(fertilityHistory)
        }
    }

    private fun saveFertilityHistory(fertilityList: List<String>) {
        val updatedFertility = fertilityList.joinToString(",")
        sharedPreferences.edit().putString(fertilityListKey, updatedFertility).apply()
    }

    private fun resetCalculation() {
        binding.apply {
            etUserName.text?.clear()
            etBasalBodyTemp.text?.clear()
            setDefaultValues()
            updateFertilityCalculation()
        }
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.fertilityResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "fertility_result_screenshot.png")
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

        startActivity(Intent.createChooser(shareIntent, "Share Fertility Information"))
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

    // Data classes
    data class CyclePhase(val name: String, val description: String) {
        companion object {
            val MENSTRUAL = CyclePhase("Menstrual", "Day 1-5: Menstruation occurs. Fertility is very low.")
            val FOLLICULAR = CyclePhase("Follicular", "Day 6-13: Eggs mature in ovaries. Estrogen rises, preparing for ovulation.")
            val OVULATION = CyclePhase("Ovulation", "Day 14-16: Mature egg is released. Peak fertility window.")
            val LUTEAL = CyclePhase("Luteal", "Day 17-28: Corpus luteum produces progesterone. Fertility decreases.")
        }
    }

    data class FertilityStatus(
        val status: String,
        val advice: String,
        val color: Int
    )
}