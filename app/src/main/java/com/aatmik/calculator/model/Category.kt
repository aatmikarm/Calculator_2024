package com.aatmik.calculator.model

data class Category(
    val name: String,
    val icon: Int,
    val description: String,
    val isSelected: Boolean = false
)