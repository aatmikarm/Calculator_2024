package com.aatmik.calculator.model

data class MultiplicationRow(
    val tableNumber: Int,
    val multiplier: Int,
    val result: Int
) {
    fun getEquationString(): String {
        return "$tableNumber × $multiplier = $result"
    }
}