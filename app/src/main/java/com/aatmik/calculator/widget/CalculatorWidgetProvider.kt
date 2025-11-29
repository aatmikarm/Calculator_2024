package com.aatmik.calculator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aatmik.calculator.R
import com.aatmik.calculator.activity.ContainerActivity
import com.aatmik.calculator.util.CalculationUtil

class CalculatorWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_BUTTON_CLICK = "com.aatmik.calculator.BUTTON_CLICK"
        private const val EXTRA_BUTTON_VALUE = "button_value"
        private const val PREFS_NAME = "CalculatorWidget"
        private const val PREF_EXPRESSION = "expression"

        // Update all widgets
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, CalculatorWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CalculatorWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_BUTTON_CLICK) {
            val buttonValue = intent.getStringExtra(EXTRA_BUTTON_VALUE) ?: return
            handleButtonClick(context, buttonValue)
        }
    }

    private fun handleButtonClick(context: Context, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var expression = prefs.getString(PREF_EXPRESSION, "") ?: ""

        expression = when (value) {
            "C" -> ""
            "⌫" -> if (expression.isNotEmpty()) expression.dropLast(1) else ""
            "=" -> {
                try {
                    val result = CalculationUtil.evaluate(expression).toDouble() // Convert Float to Double
                    if (result.isFinite()) {
                        formatResult(result)
                    } else {
                        "Error"
                    }
                } catch (e: Exception) {
                    "Error"
                }
            }
            "( )" -> {
                val openCount = expression.count { it == '(' }
                val closeCount = expression.count { it == ')' }
                expression + if (openCount > closeCount &&
                    expression.isNotEmpty() &&
                    expression.last() !in listOf('(', '+', '-', '*', '/')) {
                    ")"
                } else {
                    "("
                }
            }
            "%" -> {
                if (expression.isNotEmpty()) {
                    val lastNumber = getLastNumber(expression)
                    if (lastNumber.isNotEmpty()) {
                        val number = lastNumber.toDoubleOrNull()
                        if (number != null) {
                            val percentValue = number / 100.0
                            expression.dropLast(lastNumber.length) + formatResult(percentValue)
                        } else expression
                    } else expression
                } else expression
            }
            else -> expression + value
        }

        // Save expression
        prefs.edit().putString(PREF_EXPRESSION, expression).apply()

        // Update widget
        updateAllWidgets(context)
    }

    private fun getLastNumber(expression: String): String {
        return expression.split(Regex("[+\\-*/()]")).lastOrNull() ?: ""
    }

    private fun formatResult(result: Double): String {
        return when {
            result.isInfinite() -> if (result > 0) "∞" else "-∞"
            result.isNaN() -> "Error"
            result == 0.0 -> "0"
            result == result.toInt().toDouble() -> result.toInt().toString()
            else -> {
                val formatted = String.format("%.12f", result).trimEnd('0').trimEnd('.')
                if (formatted.length > 15) String.format("%.6E", result) else formatted
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expression = prefs.getString(PREF_EXPRESSION, "0") ?: "0"

        val views = RemoteViews(context.packageName, R.layout.widget_calculator)

        // Set display text
        views.setTextViewText(R.id.widgetDisplay, if (expression.isEmpty()) "0" else expression)

        // Set up button clicks
        setupButtonClick(context, views, R.id.widgetBtn0, "0")
        setupButtonClick(context, views, R.id.widgetBtn1, "1")
        setupButtonClick(context, views, R.id.widgetBtn2, "2")
        setupButtonClick(context, views, R.id.widgetBtn3, "3")
        setupButtonClick(context, views, R.id.widgetBtn4, "4")
        setupButtonClick(context, views, R.id.widgetBtn5, "5")
        setupButtonClick(context, views, R.id.widgetBtn6, "6")
        setupButtonClick(context, views, R.id.widgetBtn7, "7")
        setupButtonClick(context, views, R.id.widgetBtn8, "8")
        setupButtonClick(context, views, R.id.widgetBtn9, "9")
        setupButtonClick(context, views, R.id.widgetBtnAdd, "+")
        setupButtonClick(context, views, R.id.widgetBtnSubtract, "-")
        setupButtonClick(context, views, R.id.widgetBtnMultiply, "*")
        setupButtonClick(context, views, R.id.widgetBtnDivide, "/")
        setupButtonClick(context, views, R.id.widgetBtnDot, ".")
        setupButtonClick(context, views, R.id.widgetBtnClear, "C")
        setupButtonClick(context, views, R.id.widgetBtnDelete, "⌫")
        setupButtonClick(context, views, R.id.widgetBtnEquals, "=")
        setupButtonClick(context, views, R.id.widgetBtnBracket, "( )")
        setupButtonClick(context, views, R.id.widgetBtnPercent, "%")

        // Click on display to open full app
        val openAppIntent = Intent(context, ContainerActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetDisplay, openAppPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun setupButtonClick(
        context: Context,
        views: RemoteViews,
        buttonId: Int,
        value: String
    ) {
        val intent = Intent(context, CalculatorWidgetProvider::class.java)
        intent.action = ACTION_BUTTON_CLICK
        intent.putExtra(EXTRA_BUTTON_VALUE, value)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            buttonId, // Use unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(buttonId, pendingIntent)
    }
}