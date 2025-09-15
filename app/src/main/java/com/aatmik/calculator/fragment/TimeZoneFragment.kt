// Fixed Simple TimeZoneFragment.kt
package com.aatmik.calculator.fragment

import android.os.Build
import android.os.Bundle
import android.text.TextWatcher
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentTimeZoneBinding
import com.aatmik.calculator.util.TimeZoneUtils
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class TimeZoneFragment : Fragment() {

    private lateinit var binding: FragmentTimeZoneBinding

    // Current selected timezones
    private var currentReferenceTimeZone: String = TimeZone.getDefault().id
    private var selectedCompareTimeZone: String = "Europe/London" // Default comparison

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimeZoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupCurrentLocationSpinner()
        setupCompareLocationSpinner()
        setupSeekBar()

        // Initial calculation
        performTimeCalculations()
    }

    private fun setupUI() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Make current location clickable
            cardCurrentLocation.setOnClickListener {
                if (spinnerCurrentLocation.visibility == View.GONE) {
                    spinnerCurrentLocation.visibility = View.VISIBLE
                    textChangeLocationHint.visibility = View.GONE
                } else {
                    spinnerCurrentLocation.visibility = View.GONE
                    textChangeLocationHint.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupCurrentLocationSpinner() {
        val allTimeZones = TimeZoneUtils.getAllCountryTimeZones()
        val timeZoneNames = allTimeZones.map { TimeZoneUtils.getDisplayName(it) }

        val currentLocationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeZoneNames
        )
        currentLocationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCurrentLocation.apply {
            adapter = currentLocationAdapter

            // Find and set current timezone
            val currentTimeZoneId = currentReferenceTimeZone
            val currentIndex = allTimeZones.indexOfFirst { it.timeZoneId == currentTimeZoneId }
            if (currentIndex != -1) {
                setSelection(currentIndex)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentReferenceTimeZone = allTimeZones[position].timeZoneId

                    // Hide spinner and recalculate everything
                    binding.spinnerCurrentLocation.visibility = View.GONE
                    binding.textChangeLocationHint.visibility = View.VISIBLE

                    performTimeCalculations()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupCompareLocationSpinner() {
        var currentTimeZones = TimeZoneUtils.getAllCountryTimeZones()

        fun updateSpinner(timeZones: List<TimeZoneUtils.CountryTimeZone>) {
            val timeZoneNames = mutableListOf("Select country to compare...").apply {
                addAll(timeZones.map { TimeZoneUtils.getDisplayName(it) })
            }

            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                timeZoneNames
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.spinnerCountries.adapter = spinnerAdapter
        }

        // Initial setup
        updateSpinner(currentTimeZones)

        // Search functionality
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val searchQuery = s.toString()
                val filteredTimeZones = TimeZoneUtils.searchTimeZones(searchQuery)
                currentTimeZones = filteredTimeZones
                updateSpinner(filteredTimeZones)
            }
        })

        binding.spinnerCountries.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Skip the first header item
                    selectedCompareTimeZone = currentTimeZones[position - 1].timeZoneId
                    performTimeCalculations()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSeekBar() {
        binding.rangeSlider.apply {
            max = 23
            progress = ZonedDateTime.now().hour

            updateSelectedHourDisplay(progress)

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        updateSelectedHourDisplay(progress)
                        updateTimeRangeVisualization(progress)
                        performTimeCalculationsForHour(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun updateSelectedHourDisplay(hour: Int) {
        val timeFormat = String.format(Locale.getDefault(), "%02d:00", hour)
        binding.textSelectedHour.text = timeFormat
    }

    // MAIN CALCULATION FUNCTION - Called every time something changes
    private fun performTimeCalculations() {
        updateCurrentLocationDisplay()
        updateComparisonDisplay()
        updateTimeRangeVisualization(binding.rangeSlider.progress)
    }

    private fun performTimeCalculationsForHour(hour: Int) {
        updateComparisonDisplayForHour(hour)
    }


    private fun updateCurrentLocationDisplay() {
        val currentZone = ZoneId.of(currentReferenceTimeZone)
        val currentTime = ZonedDateTime.now(currentZone)

        binding.apply {
            textCurrentCity.text = TimeZoneUtils.getCityName(currentReferenceTimeZone)
            textCurrentTime.text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            textCurrentDate.text = currentTime.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))

            // Update time period UI
            updateTimePeriodUI(currentTime.hour)
        }
    }

    private fun updateComparisonDisplay() {
        val referenceZone = ZoneId.of(currentReferenceTimeZone)
        val compareZone = ZoneId.of(selectedCompareTimeZone)

        val referenceTime = ZonedDateTime.now(referenceZone)
        val compareTime = ZonedDateTime.now(compareZone)

        val timeDifference = compareTime.offset.totalSeconds - referenceTime.offset.totalSeconds
        val hoursDifference = timeDifference / 3600

        // Update comparison card
        updateComparisonCard(compareTime, hoursDifference.toInt())
    }

    private fun updateComparisonDisplayForHour(hour: Int) {
        val referenceZone = ZoneId.of(currentReferenceTimeZone)
        val compareZone = ZoneId.of(selectedCompareTimeZone)

        val referenceTime = ZonedDateTime.now(referenceZone).withHour(hour).withMinute(0)
        val compareTime = ZonedDateTime.now(compareZone).withHour(hour + (compareZone.rules.getOffset(java.time.Instant.now()).totalSeconds - referenceZone.rules.getOffset(java.time.Instant.now()).totalSeconds) / 3600).withMinute(0)

        val timeDifference = compareTime.offset.totalSeconds - referenceTime.offset.totalSeconds
        val hoursDifference = timeDifference / 3600

        updateComparisonCard(compareTime, hoursDifference.toInt())
    }

    private fun updateComparisonCard(compareTime: ZonedDateTime, hoursDifference: Int) {
        binding.apply {
            // Show comparison card
            cardComparison.visibility = View.VISIBLE

            textCompareCity.text = TimeZoneUtils.getCityName(selectedCompareTimeZone)
            textCompareCountry.text = TimeZoneUtils.getCountryName(selectedCompareTimeZone)
            textCompareTime.text = compareTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            textCompareDate.text = compareTime.format(DateTimeFormatter.ofPattern("MMM dd"))

            // Time difference display
            when {
                hoursDifference > 0 -> {
                    textTimeDifference.text = "+${hoursDifference}h ahead"
                    textTimeDifference.setTextColor(requireContext().getColor(R.color.ahead_color))
                    imageTimeDifference.setImageResource(R.drawable.ic_arrow_forward)
                    imageTimeDifference.setColorFilter(requireContext().getColor(R.color.ahead_color))
                }
                hoursDifference < 0 -> {
                    val absHours = Math.abs(hoursDifference)
                    textTimeDifference.text = "${absHours}h behind"
                    textTimeDifference.setTextColor(requireContext().getColor(R.color.behind_color))
                    imageTimeDifference.setImageResource(R.drawable.ic_arrow_back)
                    imageTimeDifference.setColorFilter(requireContext().getColor(R.color.behind_color))
                }
                else -> {
                    textTimeDifference.text = "Same time"
                    textTimeDifference.setTextColor(requireContext().getColor(R.color.same_time_color))
                    imageTimeDifference.setImageResource(R.drawable.ic_equal)
                    imageTimeDifference.setColorFilter(requireContext().getColor(R.color.same_time_color))
                }
            }

            // Update comparison time period
            updateComparisonTimePeriodUI(compareTime.hour)
        }
    }

    private fun updateTimePeriodUI(hour: Int) {
        binding.apply {
            when (hour) {
                in 6..11 -> {
                    imageTimePeriod.setImageResource(R.drawable.ic_morning_sun)
                    cardTimePeriod.setCardBackgroundColor(
                        requireContext().getColor(R.color.morning_gradient)
                    )
                    textTimePeriod.text = "Good Morning"
                }
                in 12..17 -> {
                    imageTimePeriod.setImageResource(R.drawable.ic_afternoon_sun)
                    cardTimePeriod.setCardBackgroundColor(
                        requireContext().getColor(R.color.afternoon_gradient)
                    )
                    textTimePeriod.text = "Good Afternoon"
                }
                in 18..21 -> {
                    imageTimePeriod.setImageResource(R.drawable.ic_evening_sunset)
                    cardTimePeriod.setCardBackgroundColor(
                        requireContext().getColor(R.color.evening_gradient)
                    )
                    textTimePeriod.text = "Good Evening"
                }
                else -> {
                    imageTimePeriod.setImageResource(R.drawable.ic_night_moon)
                    cardTimePeriod.setCardBackgroundColor(
                        requireContext().getColor(R.color.night_gradient)
                    )
                    textTimePeriod.text = "Good Night"
                }
            }
        }
    }

    private fun updateComparisonTimePeriodUI(hour: Int) {
        binding.apply {
            when (hour) {
                in 6..11 -> {
                    imageCompareTimePeriod.setImageResource(R.drawable.ic_morning_sun)
                    cardComparison.setCardBackgroundColor(requireContext().getColor(R.color.morning_light))
                }
                in 12..17 -> {
                    imageCompareTimePeriod.setImageResource(R.drawable.ic_afternoon_sun)
                    cardComparison.setCardBackgroundColor(requireContext().getColor(R.color.afternoon_light))
                }
                in 18..21 -> {
                    imageCompareTimePeriod.setImageResource(R.drawable.ic_evening_sunset)
                    cardComparison.setCardBackgroundColor(requireContext().getColor(R.color.evening_light))
                }
                else -> {
                    imageCompareTimePeriod.setImageResource(R.drawable.ic_night_moon)
                    cardComparison.setCardBackgroundColor(requireContext().getColor(R.color.night_light))
                }
            }
        }
    }

    private fun updateTimeRangeVisualization(selectedHour: Int) {
        binding.timeRangeContainer.removeAllViews()

        for (i in 0..23) {
            val timeBlock = createTimeBlockView(i, selectedHour)
            binding.timeRangeContainer.addView(timeBlock)
        }
    }

    private fun createTimeBlockView(hour: Int, selectedHour: Int): View {
        val timeBlock = View(requireContext())
        val layoutParams = ViewGroup.LayoutParams(60, 50)
        timeBlock.layoutParams = layoutParams

        val backgroundColor = when {
            hour == selectedHour -> requireContext().getColor(android.R.color.holo_blue_dark)
            hour in 6..11 -> requireContext().getColor(R.color.morning_light)
            hour in 12..17 -> requireContext().getColor(R.color.afternoon_light)
            hour in 18..21 -> requireContext().getColor(R.color.evening_light)
            else -> requireContext().getColor(R.color.night_light)
        }

        timeBlock.setBackgroundColor(backgroundColor)
        timeBlock.setPadding(2, 2, 2, 2)

        return timeBlock
    }

    companion object {
        private const val TAG = "TimeZoneFragment"
    }
}