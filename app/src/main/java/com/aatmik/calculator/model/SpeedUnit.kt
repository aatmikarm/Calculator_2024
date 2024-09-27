package com.aatmik.calculator.model

data class SpeedUnit(
    val name: String,
    val abbreviation: String,
    val conversionFactor: Double, // Conversion factor to meters per second (m/s)
    var value: String = "",        // Editable value for the unit
)
