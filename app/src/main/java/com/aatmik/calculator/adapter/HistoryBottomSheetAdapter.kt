// adapter/HistoryBottomSheetAdapter.kt
package com.aatmik.calculator.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.ItemHistoryBinding
import com.aatmik.calculator.model.CalculationHistoryItem
import com.aatmik.calculator.model.Tag
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ButtonUtil
import com.aatmik.calculator.util.HistoryManager
import com.aatmik.calculator.util.TagManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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

                // DEBUG: Log tags
                android.util.Log.d("HistoryAdapter", "Position $position: tags = ${item.tags}, hasTags = ${item.hasTags()}")

                // ✅ Display tags (null-safe)
                if (item.hasTags()) {
                    chipGroupTags.visibility = View.VISIBLE
                    chipGroupTags.removeAllViews()

                    item.tags?.forEach { tagId ->
                        android.util.Log.d("HistoryAdapter", "Adding tag: $tagId")
                        TagManager.getTagById(itemView.context, tagId)?.let { tag ->
                            android.util.Log.d("HistoryAdapter", "Tag found: ${tag.name}")
                            val chip = createTagChip(tag, false)
                            chipGroupTags.addView(chip)
                        } ?: android.util.Log.d("HistoryAdapter", "Tag NOT found for id: $tagId")
                    }
                } else {
                    chipGroupTags.visibility = View.GONE
                    android.util.Log.d("HistoryAdapter", "No tags to display")
                }

                // ✅ Display note if exists
                if (item.hasNote()) {
                    tvNote.visibility = View.VISIBLE
                    tvNote.text = "💬 ${item.note ?: ""}"
                    btnNote.text = "Edit"
                } else {
                    tvNote.visibility = View.GONE
                    btnNote.text = "Note"
                }

                // ✅ Note button click - opens dialog with tags
                btnNote.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    showNoteWithTagsDialog(item, position)
                }

                // Reuse button
                btnReuse.setOnClickListener {
                    ButtonUtil.vibratePhone(itemView.context)
                    onReuse(item.expression)
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

        private fun createTagChip(tag: Tag, isSelectable: Boolean): Chip {
            return Chip(itemView.context).apply {
                text = tag.name
                isCheckable = isSelectable
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(tag.color))
                setTextColor(Color.WHITE)
                chipStrokeWidth = 0f
                textSize = 12f

                if (!isSelectable) {
                    // Make it look like a display chip (not clickable)
                    isClickable = false
                    isFocusable = false
                }
            }
        }

        // ✅ Show note dialog with tags
        private fun showNoteWithTagsDialog(item: CalculationHistoryItem, position: Int) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_with_tags, null)

            val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroupTags)
            val chipAddTag = dialogView.findViewById<Chip>(R.id.chipAddTag)
            val etNote = dialogView.findViewById<EditText>(R.id.etNote)

            // Set current note
            etNote.setText(item.note ?: "")

            // Get all available tags
            val allTags = TagManager.getAllTags(context)
            val selectedTagIds = (item.tags ?: emptyList()).toMutableList()

            // Add tag chips
            allTags.forEach { tag ->
                val chip = createTagChip(tag, true).apply {
                    isChecked = selectedTagIds.contains(tag.id)

                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            if (!selectedTagIds.contains(tag.id)) {
                                selectedTagIds.add(tag.id)
                            }
                        } else {
                            selectedTagIds.remove(tag.id)
                        }
                    }

                    // Add delete option for custom tags
                    if (tag.isCustom) {
                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("Delete Tag")
                                .setMessage("Delete '${tag.name}' tag? This will remove it from all calculations.")
                                .setPositiveButton("Delete") { _, _ ->
                                    TagManager.deleteCustomTag(context, tag.id)
                                    chipGroup.removeView(this)
                                    selectedTagIds.remove(tag.id)
                                    Toast.makeText(context, "Tag deleted", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                chipGroup.addView(chip)
            }

            // Add custom tag button
            chipAddTag.setOnClickListener {
                ButtonUtil.vibratePhone(context)
                showAddCustomTagDialog(context) { newTag ->
                    val chip = createTagChip(newTag, true).apply {
                        isChecked = true
                        selectedTagIds.add(newTag.id)

                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                if (!selectedTagIds.contains(newTag.id)) {
                                    selectedTagIds.add(newTag.id)
                                }
                            } else {
                                selectedTagIds.remove(newTag.id)
                            }
                        }

                        isCloseIconVisible = true
                        setOnCloseIconClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("Delete Tag")
                                .setMessage("Delete '${newTag.name}' tag?")
                                .setPositiveButton("Delete") { _, _ ->
                                    TagManager.deleteCustomTag(context, newTag.id)
                                    chipGroup.removeView(this)
                                    selectedTagIds.remove(newTag.id)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                    chipGroup.addView(chip, chipGroup.childCount - 1) // Add before "Add Tag" button
                }
            }

            // Show dialog
            AlertDialog.Builder(context)
                .setTitle("Add Note & Tags")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val newNote = etNote.text.toString().trim()
                    // Convert to List to ensure proper serialization
                    android.util.Log.d("HistoryAdapter", "Saving tags: $selectedTagIds")
                    HistoryManager.updateNoteAndTags(context, position, newNote, selectedTagIds.toList())

                    // Refresh the adapter's data from storage
                    val updatedHistory = HistoryManager.getHistory(context)
                    updateHistory(updatedHistory)

                    // Notify the adapter that this specific item changed
                    notifyItemChanged(position)

                    // Update UI immediately
                    binding.apply {
                        // Update tags
                        if (selectedTagIds.isNotEmpty()) {
                            chipGroupTags.visibility = View.VISIBLE
                            chipGroupTags.removeAllViews()
                            selectedTagIds.forEach { tagId ->
                                TagManager.getTagById(context, tagId)?.let { tag ->
                                    chipGroupTags.addView(createTagChip(tag, false))
                                }
                            }
                        } else {
                            chipGroupTags.visibility = View.GONE
                        }

                        // Update note
                        if (newNote.isNotEmpty()) {
                            tvNote.visibility = View.VISIBLE
                            tvNote.text = "💬 $newNote"
                            btnNote.text = "Edit"
                        } else {
                            tvNote.visibility = View.GONE
                            btnNote.text = "Note"
                        }
                    }

                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    AnalyticsManager.log("note_and_tags_saved",
                        "has_note" to newNote.isNotEmpty().toString(),
                        "tag_count" to selectedTagIds.size.toString())
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear All") { _, _ ->
                    HistoryManager.updateNoteAndTags(context, position, "", emptyList())

                    // Refresh the adapter's data from storage
                    val updatedHistory = HistoryManager.getHistory(context)
                    updateHistory(updatedHistory)

                    // Notify the adapter that this specific item changed
                    notifyItemChanged(position)

                    binding.apply {
                        chipGroupTags.visibility = View.GONE
                        tvNote.visibility = View.GONE
                        btnNote.text = "Note"
                    }

                    Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
                }
                .create()
                .show()
        }

        private fun showAddCustomTagDialog(context: Context, onTagCreated: (Tag) -> Unit) {
            val input = EditText(context).apply {
                hint = "Tag name (e.g., 'Groceries', 'Salary')"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setPadding(50, 40, 50, 40)
            }

            AlertDialog.Builder(context)
                .setTitle("Create Custom Tag")
                .setView(input)
                .setPositiveButton("Create") { _, _ ->
                    val tagName = input.text.toString().trim()
                    if (tagName.isNotEmpty()) {
                        val randomColor = TagManager.getRandomColor()
                        val newTag = TagManager.addCustomTag(context, tagName, randomColor)
                        onTagCreated(newTag)
                        Toast.makeText(context, "Tag '$tagName' created", Toast.LENGTH_SHORT).show()
                        AnalyticsManager.log("custom_tag_created", "name" to tagName)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun copyToClipboard(context: Context, item: CalculationHistoryItem) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val text = buildString {
                append(item.getDisplayText())
                if (item.hasTags()) {
                    val tagNames = item.tags?.mapNotNull { tagId ->
                        TagManager.getTagById(context, tagId)?.name
                    } ?: emptyList()
                    if (tagNames.isNotEmpty()) {
                        append("\nTags: ${tagNames.joinToString(", ")}")
                    }
                }
            }

            val clip = ClipData.newPlainText("Calculation", text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        private fun shareCalculation(context: Context, item: CalculationHistoryItem) {
            val shareText = buildString {
                appendLine("Calculation:")
                appendLine("${item.expression} = ${item.result}")
                appendLine()

                if (item.hasTags()) {
                    val tagNames = item.tags?.mapNotNull { tagId ->
                        TagManager.getTagById(context, tagId)?.name
                    } ?: emptyList()
                    if (tagNames.isNotEmpty()) {
                        appendLine("Tags: ${tagNames.joinToString(", ")}")
                        appendLine()
                    }
                }

                if (item.hasNote()) {
                    appendLine("Note: ${item.note ?: ""}")
                    appendLine()
                }

                appendLine("Date: ${item.getFormattedDate()}")
                appendLine("Time: ${item.getFormattedTime()}")
                appendLine()
                appendLine("Calculated with All In One Calculator")
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