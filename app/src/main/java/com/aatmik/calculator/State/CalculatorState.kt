package com.aatmik.calculator.State

import com.aatmik.calculator.action.CalculatorOperation

data class CalculatorState(
    val number1: String = "",
    val number2: String = "",
    val operation: CalculatorOperation? = null
)
