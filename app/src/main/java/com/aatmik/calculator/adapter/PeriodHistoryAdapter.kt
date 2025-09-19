package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemPeriodHistoryBinding

class PeriodHistoryAdapter(
    private val periodList: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PeriodHistoryAdapter.PeriodHistoryViewHolder>() {

    class PeriodHistoryViewHolder(private val binding: ItemPeriodHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(periodInfo: String, onItemClick: (String) -> Unit) {
            // Parse period info: "User Name - Last: 01 Sep 2025 - Next: 29 Sep 2025 - Cycle: 28d - Phase: Follicular"
            val parts = periodInfo.split(" - ")
            if (parts.size >= 5) {
                val userName = parts[0]
                val lastPeriod = parts[1].removePrefix("Last: ")
                val nextPeriod = parts[2].removePrefix("Next: ")
                val cycleLength = parts[3].removePrefix("Cycle: ")
                val currentPhase = parts[4].removePrefix("Phase: ")

                binding.apply {
                    tvUserName.text = userName
                    tvLastPeriod.text = "Last: $lastPeriod"
                    tvNextPeriod.text = "Next: $nextPeriod"
                    tvCycleInfo.text = "$cycleLength • $currentPhase"

                    // Set phase-specific colors
                    when (currentPhase) {
                        "Menstrual" -> tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                        "Follicular" -> tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                        "Ovulation" -> tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                        "Luteal" -> tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_purple))
                        else -> tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    }

                    // Set click listener
                    root.setOnClickListener {
                        onItemClick(periodInfo)
                    }
                }
            } else {
                // Fallback for malformed data
                binding.apply {
                    tvUserName.text = periodInfo
                    tvLastPeriod.text = ""
                    tvNextPeriod.text = ""
                    tvCycleInfo.text = ""

                    root.setOnClickListener {
                        onItemClick(periodInfo)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodHistoryViewHolder {
        val binding = ItemPeriodHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PeriodHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PeriodHistoryViewHolder, position: Int) {
        holder.bind(periodList[position], onItemClick)
    }

    override fun getItemCount(): Int = periodList.size

    fun getItem(position: Int): String = periodList[position]

    fun removeItem(position: Int) {
        periodList.removeAt(position)
        notifyItemRemoved(position)
    }
}