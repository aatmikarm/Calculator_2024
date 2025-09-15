package com.aatmik.calculator.model

data class TimeZoneData(
    val timeZoneId: String,
    val cityName: String,
    val countryName: String,
    val currentTime: String,
    val currentDate: String,
    val hour: Int,
    val offsetFromUTC: String,
    val timeDifferenceFromUser: Int, // in hours
    val isAhead: Boolean, // true if ahead, false if behind
    val isCurrent: Boolean = false
)