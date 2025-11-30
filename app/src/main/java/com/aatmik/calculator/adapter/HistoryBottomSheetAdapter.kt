// adapter/HistoryBottomSheetAdapter.kt
package com.aatmik.calculator.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemHistoryBinding
import com.aatmik.calculator.model.CalculationHistoryItem
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ButtonUtil
import com.aatmik.calculator.util.HistoryManager

class HistoryBottomSheetAdapter(
    private var historyList: List<CalculationHistoryItem>,
    private val onReuse: (String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<HistoryBottomSheetAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalculationHistoryItem, position: Int) {
            binding.apply {
                // Set date and time
                tvDate.text = item.getFormattedDate()
                tvTime.text = item.getFormattedTime()
                tvExpression.text = item.expression
                tvResult.text = "= ${item.result}"

                // ✅ Display note if exists
                if (item.hasNote()) {
                    tvNote.visibility = View.VISIBLE
                    tvNote.text = "💬 ${item.note ?: ""}"  // ✅ Safe call with default
                    btnNote.text = "Edit Note"
                } else {
                    tvNote.visibility = View.GONE
                    btnNote.text = "Add Note"
                }

                // ✅ Note button click
                btnNote.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    showNoteDialog(item, position)
                }

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

        // ✅ Show note dialog
        private fun showNoteDialog(item: CalculationHistoryItem, position: Int) {
            val context = itemView.context
            val currentNote = item.note ?: ""  // ✅ Safe call with default

            val editText = EditText(context).apply {
                setText(currentNote)
                hint = "Add a note (e.g., 'Monthly expenses', 'Tip calculation')"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                maxLines = 5
                setPadding(50, 40, 50, 40)
            }

            val dialogBuilder = AlertDialog.Builder(context)
                .setTitle(if (currentNote.isEmpty()) "Add Note" else "Edit Note")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val newNote = editText.text.toString().trim()
                    HistoryManager.updateNote(context, position, newNote)

                    // Update UI immediately
                    binding.apply {
                        if (newNote.isNotEmpty()) {
                            tvNote.visibility = View.VISIBLE
                            tvNote.text = "💬 $newNote"
                            btnNote.text = "Edit Note"
                        } else {
                            tvNote.visibility = View.GONE
                            btnNote.text = "Add Note"
                        }
                    }

                    Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    AnalyticsManager.log("note_saved", "has_content" to newNote.isNotEmpty().toString())
                }
                .setNegativeButton("Cancel", null)

            // ✅ Add delete button if note exists
            if (currentNote.isNotEmpty()) {
                dialogBuilder.setNeutralButton("Delete Note") { _, _ ->
                    HistoryManager.updateNote(context, position, "")

                    // Update UI immediately
                    binding.apply {
                        tvNote.visibility = View.GONE
                        btnNote.text = "Add Note"
                    }

                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                    AnalyticsManager.log("note_deleted")
                }
            }

            dialogBuilder.create().show()
        }

        private fun copyToClipboard(context: Context, item: CalculationHistoryItem) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            // ✅ Use getDisplayText() to include note if exists
            val text = item.getDisplayText()

            val clip = ClipData.newPlainText("Calculation", text)
            clipboard.setPrimaryClip(clip)

            val message = if (item.hasNote()) {
                "Copied calculation with note"
            } else {
                "Copied to clipboard"
            }

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        private fun shareCalculation(context: Context, item: CalculationHistoryItem) {
            val shareText = buildString {
                appendLine("Calculation:")
                appendLine("${item.expression} = ${item.result}")
                appendLine()

                // ✅ Include note if exists - safe call
                if (item.hasNote()) {
                    appendLine("Note: ${item.note ?: ""}")  // ✅ Safe call with default
                    appendLine()
                }

                appendLine("Date: ${item.getFormattedDate()}")
                appendLine("Time: ${item.getFormattedTime()}")
                appendLine()
                appendLine("Calculated with All In One Calculator 2025")
            }

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