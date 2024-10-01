package com.aatmik.calculator.model

data class Currency(
    val code: String,
    val name: String,
    val rate: String? = null,
    var inputAmount: Double = 0.0, // New property to hold the input amount
)
