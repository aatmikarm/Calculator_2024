// util/HistoryManager.kt
package com.aatmik.calculator.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HistoryManager {
    private const val PREF_NAME = "calculator_history"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_HISTORY_SIZE = 100

    private val gson = Gson()

    fun saveCalculation(context: Context, expression: String, result: String) {
        val history = getHistory(context).toMutableList()
        history.add(0, com.aatmik.calculator.model.CalculationHistoryItem(expression, result))

        // Keep only last 100 calculations
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
    }

    fun getHistory(context: Context): List<com.aatmik.calculator.model.CalculationHistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<com.aatmik.calculator.model.CalculationHistoryItem>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun deleteHistoryItem(context: Context, position: Int) {
        val history = getHistory(context).toMutableList()
        if (position in history.indices) {
            history.removeAt(position)
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
        }
    }
}