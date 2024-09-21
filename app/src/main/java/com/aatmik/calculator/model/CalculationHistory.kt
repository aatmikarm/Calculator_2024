package com.aatmik.calculator.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CalculationHistory(
    val expression: String,
    val result: String,
) : Parcelable
