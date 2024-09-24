package com.aatmik.calculator.model

data class WeightUnit(
    val name: String,
    val symbol: String,
    val conversionFactor: Double, // Conversion factor to kilograms
    var value: String = "",        // Editable value for the unit
)