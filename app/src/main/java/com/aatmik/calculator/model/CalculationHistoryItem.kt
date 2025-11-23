// model/CalculationHistoryItem.kt
package com.aatmik.calculator.model

data class CalculationHistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun getFormattedTime(): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}