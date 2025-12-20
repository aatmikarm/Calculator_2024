package com.aatmik.calculator.util

import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.model.LengthUnit
import com.aatmik.calculator.model.SpeedUnit
import com.aatmik.calculator.model.WeightUnit

object CalculatorUtils {

    val calculatorList: ArrayList<Calculator> by lazy {
        createCalculatorList()
    }

    val lengthUnitList: ArrayList<LengthUnit> by lazy {
        createLengthList()
    }

    val weightUnitList: ArrayList<WeightUnit> by lazy {
        createWeightList()
    }

    val speedUnitList: ArrayList<SpeedUnit> by lazy {
        createSpeedList()
    }

    private fun createCalculatorList(): ArrayList<Calculator> {
        return arrayListOf(
            Calculator("Calculator", R.drawable.calculator_dark_logo),
            Calculator("Convertor", R.drawable.convert),
            Calculator("Currency Converter", R.drawable.dollor),
            Calculator("Percentage", R.drawable.percentage),
            Calculator("Age", R.drawable.cake),
            Calculator("Tip", R.drawable.tip),
            Calculator("Body Mass Index", R.drawable.bmi),
            Calculator("FD Calculator", R.drawable.saving),
            Calculator("Loan Calculator", R.drawable.dollor),
            Calculator("Interest Calculator", R.drawable.percentage),
            Calculator("GST Calculator", R.drawable.gst),
            Calculator("Temperature", R.drawable.temp),
            Calculator("Length", R.drawable.length),
            Calculator("Weight", R.drawable.weight),
            Calculator("Area Calculator", R.drawable.shapes_new),
            Calculator("Calorie Calculator", R.drawable.calories),
            Calculator("Investment Calculator", R.drawable.saving),
            Calculator("Tax Calculator", R.drawable.tax),
            Calculator("Speed", R.drawable.speed),
            Calculator("Fuel Economy Calculator", R.drawable.speed),
            Calculator("GPA Calculator", R.drawable.percentage),
            Calculator("Budget Calculator", R.drawable.dollor),
            Calculator("Stopwatch", R.drawable.stopwatch),
            Calculator("Fraction Calculator", R.drawable.fraction),
            Calculator("Ratio Calculator", R.drawable.ratio),
            Calculator("ROI Calculator", R.drawable.roi),
            Calculator("Break-Even Analysis", R.drawable.roi),
            Calculator("NPV & IRR Calculator", R.drawable.saving),
            Calculator("Loan Comparison", R.drawable.loan),
            Calculator("Shapes", R.drawable.shapes_new),
            Calculator("Equation", R.drawable.equation_xy),
            Calculator("Time Zone Converter", R.drawable.time_zone),
            Calculator("Proportion Calculator", R.drawable.proportion),
            Calculator("Trip Estimate", R.drawable.trip),
            Calculator("LCM & GCD Calculator", R.drawable.lcm_hcf),
            Calculator("Number Tables", R.drawable.multiplication),
            Calculator("Pregnancy Calculator", R.drawable.pregnant),
            Calculator("Period Calculator", R.drawable.period),
            Calculator("Ovulation & Fertility", R.drawable.fertilization),
            Calculator("Punnett Square", R.drawable.punnett_square),
            Calculator("Cell Dilution", R.drawable.blood_test),
            Calculator("Contribution Calculator", R.drawable.saving),
            Calculator("Ohm's Law", R.drawable.electric),
            Calculator("Beam Calculator", R.drawable.shapes_new),
            Calculator("Column Buckling", R.drawable.bodies),
            Calculator("Voltage Divider", R.drawable.electric),
            Calculator("Function Grapher", R.drawable.algebra),
            Calculator("Statistical Graph Generator", R.drawable.statistical),
            Calculator("Coordinate Geometry", R.drawable.geometry),
            Calculator("Free Fall", R.drawable.length),
            Calculator("Bodies", R.drawable.bodies),
            Calculator("Lightning Calculator", R.drawable.electric),
            Calculator("Compass", R.drawable.compass),
            Calculator("Level", R.drawable.level),
            Calculator("Molarity Calculator", R.drawable.molarity),
            Calculator("pH Calculator", R.drawable.ph),
            Calculator("Stoichiometry Calculator", R.drawable.stoichiometry),
            Calculator("Ideal Gas Law", R.drawable.gas),
            Calculator("Love Calculator", R.drawable.love),
            Calculator("Friendship Calculator", R.drawable.friends),
            Calculator("Roll a Dice", R.drawable.dice_3),
            Calculator("Random Number Generator", R.drawable.random)
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
            LengthUnit("Nanometer", "nm", 1e-9),
            LengthUnit("Micrometer", "µm", 1e-6),
            LengthUnit("Light Year", "ly", 9.461e15),
        )
    }

    private fun createWeightList(): ArrayList<WeightUnit> {
        return arrayListOf(
            WeightUnit("Kilogram", "kg", 1.0),
            WeightUnit("Gram", "g", 0.001),
            WeightUnit("Milligram", "mg", 1e-6),
            WeightUnit("Microgram", "µg", 1e-9),
            WeightUnit("Metric Ton", "t", 1000.0),
            WeightUnit("Pound", "lb", 0.453592),
            WeightUnit("Ounce", "oz", 0.0283495),
            WeightUnit("Stone", "st", 6.35029),
            WeightUnit("Imperial Ton", "ton", 1016.05),
            WeightUnit("US Ton", "ton", 907.185),
            WeightUnit("Carat", "ct", 0.0002),
        )
    }

    private fun createSpeedList(): ArrayList<SpeedUnit> {
        return arrayListOf(
            SpeedUnit("Meters per second", "m/s", 1.0),
            SpeedUnit("Kilometers per hour", "km/h", 0.277778),
            SpeedUnit("Miles per hour", "mph", 0.44704),
            SpeedUnit("Feet per second", "ft/s", 0.3048),
            SpeedUnit("Knot", "kn", 0.514444),
            SpeedUnit("Mach (at sea level)", "Mach", 340.29),
            SpeedUnit("Centimeters per second", "cm/s", 0.01),
            SpeedUnit("Inches per second", "in/s", 0.0254)
        )
    }


}
