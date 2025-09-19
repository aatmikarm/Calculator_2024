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
import com.aatmik.calculator.adapter.PeriodHistoryAdapter
import com.aatmik.calculator.databinding.DialogSavePeriodBinding
import com.aatmik.calculator.databinding.FragmentPeriodCalculatorBinding
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

class PeriodCalculatorFragment : Fragment() {

    private var isDatePickerOpen = false
    private lateinit var sharedPreferences: SharedPreferences
    private val periodListKey = "PERIOD_LIST"
    private lateinit var adapter: PeriodHistoryAdapter
    private lateinit var binding: FragmentPeriodCalculatorBinding
    private var isCalculating = false

    companion object {
        private const val TAG = "PeriodCalculatorFragment"
        private const val DEFAULT_CYCLE_LENGTH = 28
        private const val DEFAULT_PERIOD_LENGTH = 5
        private const val OVULATION_DAY_BEFORE_NEXT_PERIOD = 14
        private const val FERTILE_WINDOW_DAYS = 6
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPeriodCalculatorBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("period_history", Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        animateView(binding.periodLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()
        setupListeners()
        setupTextWatchers()

        // Set default values and calculate
        setDefaultValues()
        updatePeriodCalculation()

        displayPeriodHistory()
        setupSwipeDelete()
    }

    private fun setDefaultValues() {
        binding.apply {
            etCycleLength.setText(DEFAULT_CYCLE_LENGTH.toString())
            etPeriodLength.setText(DEFAULT_PERIOD_LENGTH.toString())

            // Set default last period date to 10 days ago
            val defaultDate = Calendar.getInstance()
            defaultDate.add(Calendar.DAY_OF_YEAR, -10)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            lastPeriodTextView.text = dateFormat.format(defaultDate.time)

            // Set current date
            val currentDate = Calendar.getInstance().time
            todayTextView.text = dateFormat.format(currentDate)
        }
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Cycle Type Spinner
            val cycleTypes = arrayOf("Regular Cycle", "Irregular Cycle")
            val typeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                cycleTypes
            )
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCycleType.adapter = typeAdapter
            spinnerCycleType.setSelection(0) // Default to Regular
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

            btnLogPeriod.setOnClickListener {
                logNewPeriod()
            }

            // Date selection listeners
            lastPeriodLayout.setOnClickListener {
                showDatePicker("Last Period")
            }

            // Spinner listener
            spinnerCycleType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updatePeriodCalculation()
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
                    updatePeriodCalculation()
                }
            }
        }

        binding.apply {
            etCycleLength.addTextChangedListener(textWatcher)
            etPeriodLength.addTextChangedListener(textWatcher)
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
                    updatePeriodCalculation()
                }
            }
        }

        picker.addOnDismissListener {
            isDatePickerOpen = false
        }

        picker.show(parentFragmentManager, picker.toString())
    }

    private fun updatePeriodCalculation() {
        if (isCalculating) return
        isCalculating = true

        try {
            binding.apply {
                val cycleLength = etCycleLength.text.toString().toIntOrNull() ?: DEFAULT_CYCLE_LENGTH
                val periodLength = etPeriodLength.text.toString().toIntOrNull() ?: DEFAULT_PERIOD_LENGTH
                val isRegularCycle = spinnerCycleType.selectedItemPosition == 0

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

                // Calculate days since last period
                val daysSinceLastPeriod = ((today.timeInMillis - lastPeriod.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

                // Calculate next period date
                val nextPeriod = Calendar.getInstance().apply {
                    time = lastPeriodDate
                    add(Calendar.DAY_OF_YEAR, cycleLength)
                }

                // Calculate ovulation date (typically 14 days before next period)
                val ovulationDate = Calendar.getInstance().apply {
                    time = nextPeriod.time
                    add(Calendar.DAY_OF_YEAR, -OVULATION_DAY_BEFORE_NEXT_PERIOD)
                }

                // Calculate fertile window (5 days before ovulation + ovulation day)
                val fertileStart = Calendar.getInstance().apply {
                    time = ovulationDate.time
                    add(Calendar.DAY_OF_YEAR, -(FERTILE_WINDOW_DAYS - 1))
                }
                val fertileEnd = Calendar.getInstance().apply {
                    time = ovulationDate.time
                    add(Calendar.DAY_OF_YEAR, 1)
                }

                // Days until next period
                val daysUntilNextPeriod = ((nextPeriod.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

                // Update UI
                tvDaysSinceLastPeriod.text = daysSinceLastPeriod.toString()
                tvDaysUntilNextPeriod.text = if (daysUntilNextPeriod > 0) daysUntilNextPeriod.toString() else "Overdue"

                // Update cycle progress
                val cycleProgress = (daysSinceLastPeriod.toFloat() / cycleLength * 100).coerceIn(0f, 100f)
                tvCycleProgress.text = "${cycleProgress.toInt()}%"

                // Update dates
                tvNextPeriodDate.text = dateFormat.format(nextPeriod.time)
                tvOvulationDate.text = dateFormat.format(ovulationDate.time)
                tvFertileStart.text = dateFormat.format(fertileStart.time)
                tvFertileEnd.text = dateFormat.format(fertileEnd.time)

                // Update cycle phase
                val currentPhase = getCurrentCyclePhase(daysSinceLastPeriod, cycleLength, periodLength)
                tvCurrentPhase.text = currentPhase.name
                tvPhaseDescription.text = currentPhase.description

                // Update period status
                updatePeriodStatus(daysUntilNextPeriod, daysSinceLastPeriod, periodLength)

                // Update summary statistics
                updateSummaryStats(cycleLength, periodLength, daysSinceLastPeriod, isRegularCycle)

                // Update detailed information
                updateDetailedInfo(currentPhase, cycleLength, periodLength, isRegularCycle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating period information", e)
        } finally {
            isCalculating = false
        }
    }

    private fun getCurrentCyclePhase(daysSinceLast: Int, cycleLength: Int, periodLength: Int): CyclePhase {
        return when {
            daysSinceLast <= periodLength -> CyclePhase.MENSTRUAL
            daysSinceLast <= periodLength + 7 -> CyclePhase.FOLLICULAR
            daysSinceLast in (cycleLength - 16)..(cycleLength - 12) -> CyclePhase.OVULATION
            daysSinceLast > cycleLength - 14 -> CyclePhase.LUTEAL
            else -> CyclePhase.FOLLICULAR
        }
    }

    private fun updatePeriodStatus(daysUntilNext: Int, daysSinceLast: Int, periodLength: Int) {
        binding.apply {
            when {
                daysSinceLast <= periodLength -> {
                    tvPeriodStatus.text = "Currently on period"
                    tvPeriodStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                daysUntilNext <= 0 -> {
                    tvPeriodStatus.text = "Period is overdue"
                    tvPeriodStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                }
                daysUntilNext <= 3 -> {
                    tvPeriodStatus.text = "Period starting soon"
                    tvPeriodStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                }
                else -> {
                    tvPeriodStatus.text = "Normal cycle"
                    tvPeriodStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
            }
        }
    }

    private fun updateSummaryStats(cycleLength: Int, periodLength: Int, daysSinceLast: Int, isRegular: Boolean) {
        binding.apply {
            tvCycleLengthSummary.text = "$cycleLength days"
            tvPeriodLengthSummary.text = "$periodLength days"
            tvCycleTypeSummary.text = if (isRegular) "Regular" else "Irregular"

            // Calculate average statistics (simplified)
            val periodsPerYear = 365 / cycleLength
            tvPeriodsPerYear.text = periodsPerYear.toString()

            // Days in current cycle phase
            val daysInPhase = when (getCurrentCyclePhase(daysSinceLast, cycleLength, periodLength)) {
                CyclePhase.MENSTRUAL -> daysSinceLast
                CyclePhase.FOLLICULAR -> daysSinceLast - periodLength
                CyclePhase.OVULATION -> daysSinceLast - (cycleLength - 16)
                CyclePhase.LUTEAL -> daysSinceLast - (cycleLength - 14)
                else -> daysSinceLast - periodLength // Default to follicular calculation
            }
            tvDaysInCurrentPhase.text = maxOf(0, daysInPhase).toString()
        }
    }

    private fun updateDetailedInfo(phase: CyclePhase, cycleLength: Int, periodLength: Int, isRegular: Boolean) {
        binding.apply {
            val info = StringBuilder()
            info.append("Menstrual Cycle Information\n\n")

            // Current phase info
            info.append("Current Phase: ${phase.name}\n")
            info.append("${phase.description}\n\n")

            // Cycle characteristics
            info.append("Cycle Characteristics:\n")
            info.append("• Cycle Length: $cycleLength days\n")
            info.append("• Period Duration: $periodLength days\n")
            info.append("• Cycle Type: ${if (isRegular) "Regular" else "Irregular"}\n\n")

            // Phase breakdown
            info.append("Typical Cycle Phases:\n")
            info.append("• Menstrual Phase: Days 1-$periodLength\n")
            info.append("• Follicular Phase: Days ${periodLength + 1}-${cycleLength - 14}\n")
            info.append("• Ovulation: Around day ${cycleLength - 14}\n")
            info.append("• Luteal Phase: Days ${cycleLength - 13}-$cycleLength\n\n")

            // Fertility information
            info.append("Fertility Information:\n")
            info.append("• Most fertile days are typically 5 days before ovulation through ovulation day\n")
            info.append("• Ovulation usually occurs 14 days before next period\n")
            info.append("• Sperm can survive up to 5 days in reproductive tract\n")
            info.append("• Egg survives 12-24 hours after ovulation\n\n")
            // Health tips
            info.append("General Health Tips:\n")
            info.append("• Track symptoms and patterns\n")
            info.append("• Maintain regular exercise and healthy diet\n")
            info.append("• Stay hydrated, especially during menstruation\n")
            info.append("• Consult healthcare provider for irregular cycles\n")
            info.append("• Consider talking to a doctor if cycles are consistently outside 21-35 day range\n\n")

            // Disclaimer
            info.append("Important: This calculator provides estimates based on typical cycles. ")
            info.append("Individual cycles can vary. For contraception or fertility planning, ")
            info.append("consult with a healthcare professional.")

            tvPeriodDetails.text = info.toString()
        }
    }

    private fun logNewPeriod() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.lastPeriodTextView.text = dateFormat.format(today.time)
        updatePeriodCalculation()

        // Optionally save this as a new entry
        showSaveDialog()
    }

    private fun showSaveDialog() {
        val dialogBinding = DialogSavePeriodBinding.inflate(layoutInflater)

        // Pre-fill with current user name if available
        dialogBinding.etUserName.setText(binding.etUserName.text.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Save Period Information")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val userName = dialogBinding.etUserName.text.toString()
                if (userName.isNotEmpty()) {
                    savePeriodToHistory(userName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun savePeriodToHistory(userName: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val lastPeriodDate = binding.lastPeriodTextView.text.toString()
        val nextPeriodDate = binding.tvNextPeriodDate.text.toString()
        val cycleLength = binding.etCycleLength.text.toString()
        val currentPhase = binding.tvCurrentPhase.text.toString()

        val periodInfo = "$userName - Last: $lastPeriodDate - Next: $nextPeriodDate - Cycle: ${cycleLength}d - Phase: $currentPhase"

        val periodList = getPeriodHistory().toMutableList()
        periodList.add(periodInfo)

        val editor = sharedPreferences.edit()
        editor.putString(periodListKey, periodList.joinToString(","))
        editor.apply()

        displayPeriodHistory()
    }

    private fun getPeriodHistory(): MutableList<String> {
        val savedPeriods = sharedPreferences.getString(periodListKey, "") ?: ""
        return if (savedPeriods.isNotEmpty()) {
            savedPeriods.split(",").toMutableList()
        } else {
            mutableListOf()
        }
    }

    private fun displayPeriodHistory() {
        val periodList = getPeriodHistory()

        if (periodList.isEmpty()) {
            binding.periodHistoryTextView.visibility = View.VISIBLE
            binding.periodHistoryRecyclerView.visibility = View.GONE
        } else {
            binding.periodHistoryTextView.visibility = View.GONE
            binding.periodHistoryRecyclerView.visibility = View.VISIBLE

            adapter = PeriodHistoryAdapter(periodList) { period ->
                Log.d(TAG, "Selected period: $period")
                // Handle period history item click
            }
            binding.periodHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
            binding.periodHistoryRecyclerView.adapter = adapter
        }
    }

    private fun setupSwipeDelete() {
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val period = adapter.getItem(position)
                adapter.removeItem(position)
                removePeriodFromHistory(period)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.periodHistoryRecyclerView)
    }

    private fun removePeriodFromHistory(period: String) {
        val periodHistory = getPeriodHistory()
        if (periodHistory.contains(period)) {
            periodHistory.remove(period)
            savePeriodHistory(periodHistory)
        }
    }

    private fun savePeriodHistory(periodList: List<String>) {
        val updatedPeriods = periodList.joinToString(",")
        sharedPreferences.edit().putString(periodListKey, updatedPeriods).apply()
    }

    private fun resetCalculation() {
        binding.apply {
            etUserName.text?.clear()
            setDefaultValues()
            updatePeriodCalculation()
        }
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.periodResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "period_result_screenshot.png")
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

        startActivity(Intent.createChooser(shareIntent, "Share Period Information"))
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

    // Data class for cycle phases
    data class CyclePhase(val name: String, val description: String) {
        companion object {
            val MENSTRUAL = CyclePhase("Menstrual", "Day 1-5: Menstruation occurs. Hormone levels are low.")
            val FOLLICULAR = CyclePhase("Follicular", "Day 1-13: Eggs mature in ovaries. Estrogen rises.")
            val OVULATION = CyclePhase("Ovulation", "Day 14: Mature egg is released. Most fertile time.")
            val LUTEAL = CyclePhase("Luteal", "Day 15-28: Corpus luteum produces progesterone.")
        }
    }
}