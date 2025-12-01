// model/CalculationHistoryItem.kt
package com.aatmik.calculator.model

data class CalculationHistoryItem(
    val expression: String,
    val result: String,
    val note: String? = null,
    val tags: List<String>? = null, // Nullable for backward compatibility
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted date string
     * Example: "23 Nov 2025"
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Get formatted time string
     * Example: "02:30 PM"
     */
    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    /**
     * Get formatted date and time
     * Example: "23 Nov 2025, 02:30 PM"
     */
    fun getFormattedDateTime(): String {
        return "${getFormattedDate()}, ${getFormattedTime()}"
    }

    /**
     * Check if this item has a note
     */
    fun hasNote(): Boolean {
        return !note.isNullOrEmpty()
    }

    /**
     * Check if this item has tags
     */
    fun hasTags(): Boolean {
        return tags != null && tags.isNotEmpty()
    }

    /**
     * Get display text for sharing or copying
     * Includes note if available
     */
    fun getDisplayText(): String {
        return buildString {
            append("$expression = $result")
            if (hasNote()) {
                append("\nNote: $note")
            }
        }
    }

    /**
     * Get compact display text (single line)
     * Example: "1500+2500 = 4000 (Rent + utilities)"
     */
    fun getCompactDisplayText(): String {
        return if (hasNote()) {
            "$expression = $result ($note)"
        } else {
            "$expression = $result"
        }
    }
}