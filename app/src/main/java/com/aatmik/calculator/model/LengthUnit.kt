package com.aatmik.calculator.model

data class LengthUnit(
    val name: String,
    val symbol: String,
    val conversionFactor: Double,
    var value: String = "0",
)