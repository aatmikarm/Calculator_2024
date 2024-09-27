package com.aatmik.calculator.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.aatmik.calculator.R
import com.aatmik.calculator.fragment.BasicCalculatorFragment.Companion.addedBC

object ButtonUtil {
    fun addNumberValueToText(context: Context, buttonId: Button, textViewId: TextView, id: Int?) {
        buttonId.setOnClickListener {
            vibratePhone(context)
            textViewId.text = "${textViewId.text}${buttonId.text}"
            when (id) {
                0 -> addedBC = false
            }
        }
    }

    fun addOperatorValueToText(
        context: Context,
        buttonId: Button,
        textViewId: TextView,
        text: String,
        id: Int,
    ) {
        buttonId.setOnClickListener {
            vibratePhone(context)

            when (id) {
                0 -> {
                    if (addedBC) textViewId.text =
                        textViewId.text.subSequence(0, textViewId.length() - 1)
                    textViewId.text = textViewId.text.toString() + text
                    addedBC = true
                }
            }
        }
    }

    fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(
            VibrationEffect.createPredefined(
                VibrationEffect.EFFECT_CLICK
            )
        )
        else vibrator.vibrate(10)
    }

    fun enterNumberToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.enter_number), Toast.LENGTH_SHORT).show()
    }

    fun invalidInputToast(context: Context) {
        Toast.makeText(context, context.getString(R.string.invalid_input), Toast.LENGTH_SHORT)
            .show()
    }
}