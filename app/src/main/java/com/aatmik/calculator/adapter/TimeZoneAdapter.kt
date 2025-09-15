// Enhanced TimeZoneAdapter.kt with better difference display
package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.ItemTimeZoneBinding
import com.aatmik.calculator.model.TimeZoneData

class TimeZoneAdapter(
    private val onRemoveClick: (TimeZoneData) -> Unit
) : ListAdapter<TimeZoneData, TimeZoneAdapter.TimeZoneViewHolder>(TimeZoneDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeZoneViewHolder {
        val binding = ItemTimeZoneBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeZoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeZoneViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeZoneViewHolder(
        private val binding: ItemTimeZoneBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeZone: TimeZoneData) {
            binding.apply {
                textCityName.text = timeZone.cityName
                textCountryName.text = timeZone.countryName
                textTime.text = timeZone.currentTime
                textDate.text = timeZone.currentDate
                textOffset.text = timeZone.offsetFromUTC

                // ENHANCED: Better time difference display with dynamic calculation
                when {
                    timeZone.timeDifferenceFromUser > 0 -> {
                        val hourDiff = timeZone.timeDifferenceFromUser
                        textTimeDifference.text = if (hourDiff == 1) {
                            "+${hourDiff}h ahead"
                        } else {
                            "+${hourDiff}h ahead"
                        }
                        textTimeDifference.setTextColor(binding.root.context.getColor(R.color.ahead_color))
                        imageTimeDifference.setImageResource(R.drawable.ic_arrow_forward)
                        imageTimeDifference.setColorFilter(binding.root.context.getColor(R.color.ahead_color))
                    }
                    timeZone.timeDifferenceFromUser < 0 -> {
                        val hourDiff = Math.abs(timeZone.timeDifferenceFromUser)
                        textTimeDifference.text = if (hourDiff == 1) {
                            "${hourDiff}h behind"
                        } else {
                            "${hourDiff}h behind"
                        }
                        textTimeDifference.setTextColor(binding.root.context.getColor(R.color.behind_color))
                        imageTimeDifference.setImageResource(R.drawable.ic_arrow_back)
                        imageTimeDifference.setColorFilter(binding.root.context.getColor(R.color.behind_color))
                    }
                    else -> {
                        textTimeDifference.text = "Same time"
                        textTimeDifference.setTextColor(binding.root.context.getColor(R.color.same_time_color))
                        imageTimeDifference.setImageResource(R.drawable.ic_equal)
                        imageTimeDifference.setColorFilter(binding.root.context.getColor(R.color.same_time_color))
                    }
                }

                // Set time period icon and background
                updateTimePeriodUI(timeZone.hour)

                buttonRemove.setOnClickListener {
                    onRemoveClick(timeZone)
                }
            }
        }

        private fun updateTimePeriodUI(hour: Int) {
            binding.apply {
                when (hour) {
                    in 6..11 -> {
                        imageTimePeriod.setImageResource(R.drawable.ic_morning_sun)
                        cardTimeZone.setCardBackgroundColor(root.context.getColor(R.color.morning_light))
                    }
                    in 12..17 -> {
                        imageTimePeriod.setImageResource(R.drawable.ic_afternoon_sun)
                        cardTimeZone.setCardBackgroundColor(root.context.getColor(R.color.afternoon_light))
                    }
                    in 18..21 -> {
                        imageTimePeriod.setImageResource(R.drawable.ic_evening_sunset)
                        cardTimeZone.setCardBackgroundColor(root.context.getColor(R.color.evening_light))
                    }
                    else -> {
                        imageTimePeriod.setImageResource(R.drawable.ic_night_moon)
                        cardTimeZone.setCardBackgroundColor(root.context.getColor(R.color.night_light))
                    }
                }
            }
        }
    }
}

class TimeZoneDiffCallback : DiffUtil.ItemCallback<TimeZoneData>() {
    override fun areItemsTheSame(oldItem: TimeZoneData, newItem: TimeZoneData): Boolean {
        return oldItem.timeZoneId == newItem.timeZoneId
    }

    override fun areContentsTheSame(oldItem: TimeZoneData, newItem: TimeZoneData): Boolean {
        // IMPORTANT: Check all fields including time difference for proper updates
        return oldItem.timeZoneId == newItem.timeZoneId &&
                oldItem.currentTime == newItem.currentTime &&
                oldItem.timeDifferenceFromUser == newItem.timeDifferenceFromUser &&
                oldItem.isAhead == newItem.isAhead
    }
}