package com.aatmik.calculator.util

import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.model.LengthUnit

object CalculatorUtils {

    val calculatorList: ArrayList<Calculator> by lazy {
        createCalculatorList()
    }

    val lengthUnitList: ArrayList<LengthUnit> by lazy {
        createLengthList()
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

    private fun createLengthList(): ArrayList<LengthUnit> {
        return arrayListOf(
            LengthUnit("Meter", "m", 1.0),
            LengthUnit("Kilometer", "km", 1000.0),
            LengthUnit("Centimeter", "cm", 0.01),
            LengthUnit("Millimeter", "mm", 0.001),
            LengthUnit("Inch", "in", 0.0254),
            LengthUnit("Foot", "ft", 0.3048),
            LengthUnit("Yard", "yd", 0.9144),
            LengthUnit("Mile", "mi", 1609.34),
        )
    }

}
