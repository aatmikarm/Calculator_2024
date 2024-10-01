package com.aatmik.calculator.model

data class Currency(
    val code: String,
    val name: String,
    val rate: String? = null,
    val conversionRate: String? = null,
)
