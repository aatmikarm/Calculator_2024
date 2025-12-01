// util/HistoryManager.kt
package com.aatmik.calculator.util

import android.content.Context
import android.content.SharedPreferences
import com.aatmik.calculator.model.CalculationHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HistoryManager {
    private const val PREF_NAME = "calculator_history"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY_SIZE = 100

    private val gson = Gson()

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save a new calculation to history
     * @param context Application context
     * @param expression The calculation expression (e.g., "1500+2500")
     * @param result The calculation result (e.g., "4000")
     * @param note Optional note for this calculation (e.g., "Rent + utilities")
     * @param tags List of tag IDs for this calculation
     */
    fun saveCalculation(context: Context, expression: String, result: String, note: String = "", tags: List<String>? = null) {
        val history = getHistory(context).toMutableList()

        // Create new history item with timestamp
        val calculation = CalculationHistoryItem(
            expression = expression,
            result = result,
            note = note.ifEmpty { null },
            tags = tags,
            timestamp = System.currentTimeMillis()
        )

        // Add to beginning of list (most recent first)
        history.add(0, calculation)

        // Keep only last MAX_HISTORY_SIZE calculations
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        // Save to SharedPreferences
        val json = gson.toJson(history)
        getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
    }

    /**
     * Get all calculation history
     * @param context Application context
     * @return List of calculation history items (most recent first)
     */
    fun getHistory(context: Context): List<CalculationHistoryItem> {
        val json = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<CalculationHistoryItem>>() {}.type
            gson.fromJson<List<CalculationHistoryItem>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Update note and tags for existing calculation
     * @param context Application context
     * @param position Position in the history list
     * @param newNote New note text (can be empty to remove note)
     * @param newTags New list of tag IDs
     */
    fun updateNoteAndTags(context: Context, position: Int, newNote: String, newTags: List<String>) {
        val history = getHistory(context).toMutableList()

        android.util.Log.d("HistoryManager", "Updating position $position with tags: $newTags")

        if (position in history.indices) {
            val item = history[position]
            val updatedItem = item.copy(
                note = newNote.ifEmpty { null },
                tags = if (newTags.isEmpty()) null else newTags  // Save as null if empty, otherwise as list
            )
            history[position] = updatedItem

            android.util.Log.d("HistoryManager", "Updated item tags: ${updatedItem.tags}")

            // Save updated history
            val json = gson.toJson(history)
            android.util.Log.d("HistoryManager", "Saving JSON: $json")
            getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
        }
    }

    /**
     * Update note for existing calculation
     * @param context Application context
     * @param position Position in the history list
     * @param newNote New note text (can be empty to remove note)
     */
    fun updateNote(context: Context, position: Int, newNote: String) {
        val history = getHistory(context).toMutableList()

        if (position in history.indices) {
            val item = history[position]
            history[position] = item.copy(note = newNote.ifEmpty { null })

            // Save updated history
            val json = gson.toJson(history)
            getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
        }
    }

    /**
     * Update tags for existing calculation
     * @param context Application context
     * @param position Position in the history list
     * @param newTags New list of tag IDs
     */
    fun updateTags(context: Context, position: Int, newTags: List<String>) {
        val history = getHistory(context).toMutableList()

        if (position in history.indices) {
            val item = history[position]
            history[position] = item.copy(tags = newTags)

            // Save updated history
            val json = gson.toJson(history)
            getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
        }
    }

    /**
     * Delete a specific history item
     * @param context Application context
     * @param position Position in the history list to delete
     */
    fun deleteHistoryItem(context: Context, position: Int) {
        val history = getHistory(context).toMutableList()

        if (position in history.indices) {
            history.removeAt(position)

            // Save updated history
            val json = gson.toJson(history)
            getPrefs(context).edit().putString(KEY_HISTORY, json).apply()
        }
    }

    /**
     * Clear all calculation history
     * @param context Application context
     */
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }

    /**
     * Get total count of history items
     * @param context Application context
     * @return Number of items in history
     */
    fun getHistoryCount(context: Context): Int {
        return getHistory(context).size
    }

    /**
     * Search history by expression or note
     * @param context Application context
     * @param query Search query
     * @return Filtered list of matching history items
     */
    fun searchHistory(context: Context, query: String): List<CalculationHistoryItem> {
        if (query.isBlank()) return getHistory(context)

        val lowercaseQuery = query.lowercase()
        return getHistory(context).filter { item ->
            item.expression.lowercase().contains(lowercaseQuery) ||
                    item.result.lowercase().contains(lowercaseQuery) ||
                    (item.note?.lowercase()?.contains(lowercaseQuery) == true)
        }
    }

    /**
     * Get history items with notes only
     * @param context Application context
     * @return List of history items that have notes
     */
    fun getHistoryWithNotes(context: Context): List<CalculationHistoryItem> {
        return getHistory(context).filter { it.hasNote() }
    }

    /**
     * Get history items with specific tag
     * @param context Application context
     * @param tagId Tag ID to filter by
     * @return List of history items with this tag
     */
    fun getHistoryByTag(context: Context, tagId: String): List<CalculationHistoryItem> {
        return getHistory(context).filter { it.tags?.contains(tagId) == true }
    }

    /**
     * Export history as text
     * @param context Application context
     * @return Formatted string of all history items
     */
    fun exportHistory(context: Context): String {
        val history = getHistory(context)
        if (history.isEmpty()) return "No calculation history"

        return buildString {
            appendLine("=== CALCULATION HISTORY ===")
            appendLine()

            history.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.expression} = ${item.result}")
                if (item.hasNote()) {
                    appendLine("   Note: ${item.note ?: ""}")
                }
                if (item.hasTags()) {
                    val tagNames = item.tags?.mapNotNull { tagId ->
                        TagManager.getTagById(context, tagId)?.name
                    } ?: emptyList()
                    if (tagNames.isNotEmpty()) {
                        appendLine("   Tags: ${tagNames.joinToString(", ")}")
                    }
                }
                appendLine("   ${item.getFormattedDateTime()}")
                appendLine()
            }

            appendLine("Total calculations: ${history.size}")
        }
    }
}