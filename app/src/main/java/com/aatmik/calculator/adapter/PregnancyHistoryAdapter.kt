package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemPregnancyHistoryBinding

class PregnancyHistoryAdapter(
    private val pregnancyList: MutableList<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PregnancyHistoryAdapter.PregnancyHistoryViewHolder>() {

    class PregnancyHistoryViewHolder(private val binding: ItemPregnancyHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pregnancyInfo: String, onItemClick: (String) -> Unit) {
            // Parse pregnancy info: "Mother Name - Baby: Baby Name - Week: 20 - Due: 06 Feb 2026"
            val parts = pregnancyInfo.split(" - ")
            if (parts.size >= 4) {
                val motherName = parts[0]
                val babyInfo = parts[1].removePrefix("Baby: ")
                val weekInfo = parts[2].removePrefix("Week: ")
                val dueInfo = parts[3].removePrefix("Due: ")

                binding.apply {
                    tvMotherName.text = motherName
                    tvBabyName.text = if (babyInfo == "Not specified") "Baby" else babyInfo
                    tvCurrentWeek.text = "Week $weekInfo"
                    tvDueDate.text = "Due: $dueInfo"

                    // Set click listener
                    root.setOnClickListener {
                        onItemClick(pregnancyInfo)
                    }
                }
            } else {
                // Fallback for malformed data
                binding.apply {
                    tvMotherName.text = pregnancyInfo
                    tvBabyName.text = ""
                    tvCurrentWeek.text = ""
                    tvDueDate.text = ""

                    root.setOnClickListener {
                        onItemClick(pregnancyInfo)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PregnancyHistoryViewHolder {
        val binding = ItemPregnancyHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PregnancyHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PregnancyHistoryViewHolder, position: Int) {
        holder.bind(pregnancyList[position], onItemClick)
    }

    override fun getItemCount(): Int = pregnancyList.size

    fun getItem(position: Int): String = pregnancyList[position]

    fun removeItem(position: Int) {
        pregnancyList.removeAt(position)
        notifyItemRemoved(position)
    }
}