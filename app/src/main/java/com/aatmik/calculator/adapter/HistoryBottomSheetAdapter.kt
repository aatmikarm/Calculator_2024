// adapter/HistoryBottomSheetAdapter.kt
package com.aatmik.calculator.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemHistoryBinding
import com.aatmik.calculator.model.CalculationHistoryItem
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ButtonUtil

class HistoryBottomSheetAdapter(
    private var historyList: List<CalculationHistoryItem>,
    private val onReuse: (String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<HistoryBottomSheetAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalculationHistoryItem, position: Int) {
            binding.apply {
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()
                tvExpression.text = item.expression
                tvResult.text = "= ${item.result}"

                // Reuse button
                btnReuse.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    onReuse(item.result)
                    AnalyticsManager.log("history_reused")
                }

                // Copy button
                btnCopy.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    copyToClipboard(itemView.context, item)
                    AnalyticsManager.log("history_copied")
                }

                // Share button
                btnShare.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    shareCalculation(itemView.context, item)
                    AnalyticsManager.log("history_shared")
                }

                // Long press to delete
                itemView.setOnLongClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    onDelete(position)
                    true
                }
            }
        }

        private fun copyToClipboard(context: Context, item: CalculationHistoryItem) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Calculation",
                "${item.expression} = ${item.result}"
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        private fun shareCalculation(context: Context, item: CalculationHistoryItem) {
            val shareText = """
                Calculation:
                ${item.expression} = ${item.result}
                
                Date: ${item.getFormattedDate()}
                Time: ${item.getFormattedTime()}
                
                Calculated with All In One Calculator 2025
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share calculation via"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(historyList[position], position)
    }

    override fun getItemCount() = historyList.size

    fun updateHistory(newHistory: List<CalculationHistoryItem>) {
        historyList = newHistory
        notifyDataSetChanged()
    }
}