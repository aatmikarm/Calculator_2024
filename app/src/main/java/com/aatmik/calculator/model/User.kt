package com.aatmik.calculator.model

data class User(
    val name: String,
    val dob: String, // Store as a String for simplicity, you can use Date type as well
    val age: Int
)
