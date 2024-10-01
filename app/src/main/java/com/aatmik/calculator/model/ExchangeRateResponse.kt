package com.aatmik.calculator.model

data class ExchangeRateResponse(
    val base: String,
    val rates: Map<String, Double>,
)
