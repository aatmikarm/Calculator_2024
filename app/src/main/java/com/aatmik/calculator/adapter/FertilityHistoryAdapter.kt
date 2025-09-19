package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemFertilityHistoryBinding

class FertilityHistoryAdapter(
    private val fertilityList: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<FertilityHistoryAdapter.FertilityHistoryViewHolder>() {

    class FertilityHistoryViewHolder(private val binding: ItemFertilityHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fertilityInfo: String, onItemClick: (String) -> Unit) {
            // Parse fertility info: "User Name - Period: 01 Sep 2025 - Ovulation: 15 Sep 2025 - Fertility: 25% - Phase: Ovulation - Goal: Trying to Conceive"
            val parts = fertilityInfo.split(" - ")
            if (parts.size >= 6) {
                val userName = parts[0]
                val periodDate = parts[1].removePrefix("Period: ")
                val ovulationDate = parts[2].removePrefix("Ovulation: ")
                val fertilityPercent = parts[3].removePrefix("Fertility: ")
                val currentPhase = parts[4].removePrefix("Phase: ")
                val goal = parts[5].removePrefix("Goal: ")

                binding.apply {
                    tvUserName.text = userName
                    tvPeriodDate.text = "Period: $periodDate"
                    tvOvulationDate.text = "Ovulation: $ovulationDate"
                    tvFertilityPercent.text = fertilityPercent
                    tvCycleInfo.text = "$currentPhase • ${getGoalShort(goal)}"

                    // Set fertility-specific colors based on percentage
                    val fertility = fertilityPercent.removeSuffix("%").toIntOrNull() ?: 0
                    when {
                        fertility >= 25 -> {
                            tvFertilityPercent.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                            tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                        }
                        fertility >= 15 -> {
                            tvFertilityPercent.setTextColor(itemView.context.getColor(android.R.color.holo_orange_light))
                            tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_orange_light))
                        }
                        fertility >= 5 -> {
                            tvFertilityPercent.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                            tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                        }
                        else -> {
                            tvFertilityPercent.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                            tvCycleInfo.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                        }
                    }

                    // Set click listener
                    root.setOnClickListener {
                        onItemClick(fertilityInfo)
                    }
                }
            } else {
                // Fallback for malformed data
                binding.apply {
                    tvUserName.text = fertilityInfo
                    tvPeriodDate.text = ""
                    tvOvulationDate.text = ""
                    tvFertilityPercent.text = ""
                    tvCycleInfo.text = ""

                    root.setOnClickListener {
                        onItemClick(fertilityInfo)
                    }
                }
            }
        }

        private fun getGoalShort(goal: String): String {
            return when (goal) {
                "Trying to Conceive" -> "TTC"
                "Avoiding Pregnancy" -> "Avoiding"
                "Cycle Tracking" -> "Tracking"
                else -> goal
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FertilityHistoryViewHolder {
        val binding = ItemFertilityHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FertilityHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FertilityHistoryViewHolder, position: Int) {
        holder.bind(fertilityList[position], onItemClick)
    }

    override fun getItemCount(): Int = fertilityList.size

    fun getItem(position: Int): String = fertilityList[position]

    fun removeItem(position: Int) {
        fertilityList.removeAt(position)
        notifyItemRemoved(position)
    }
}