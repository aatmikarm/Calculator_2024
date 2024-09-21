package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.CalculationHistory

class HistoryAdapter(
    private val historyList: MutableList<CalculationHistory>,
    private val onItemClick: (CalculationHistory) -> Unit,
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyList[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    // Method to add new items and notify the adapter
    fun addHistoryItem(newItem: CalculationHistory) {
        historyList.add(newItem)
        notifyItemInserted(historyList.size - 1)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExpression = itemView.findViewById<TextView>(R.id.tvExpression)
        private val tvResult = itemView.findViewById<TextView>(R.id.tvResult)

        fun bind(historyItem: CalculationHistory) {
            tvExpression.text = historyItem.expression
            tvResult.text = historyItem.result
            itemView.setOnClickListener {
                onItemClick(historyItem)
            }
        }
    }
}
