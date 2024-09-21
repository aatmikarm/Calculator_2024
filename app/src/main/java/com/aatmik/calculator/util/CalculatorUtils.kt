package com.aatmik.calculator.util

import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator

object CalculatorUtils {

    val calculatorList: ArrayList<Calculator> by lazy {
        createCalculatorList()
    }

    private fun createCalculatorList(): ArrayList<Calculator> {
        return arrayListOf(
            Calculator("Basic", R.drawable.calculator_new),
            Calculator("Convertor", R.drawable.convert),
            Calculator("Stopwatch", R.drawable.stopwatch),
            Calculator("Percentage", R.drawable.percentage),
            Calculator("Equation", R.drawable.equation_xy),
            Calculator("Shapes", R.drawable.shapes_new),
            Calculator("Bodies", R.drawable.bodies),
            Calculator("Length", R.drawable.length),
            Calculator("Speed", R.drawable.speed),
            Calculator("Temperature", R.drawable.temp),
            Calculator("Weight", R.drawable.weight),
            Calculator("Currency Converter", R.drawable.dollor),
            Calculator("Tip", R.drawable.tip),
            Calculator("Body Mass Index", R.drawable.bmi),
            Calculator("Age", R.drawable.cake)
        )
    }
}
