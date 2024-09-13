package com.aatmik.calculator.ViewModel

import androidx.lifecycle.ViewModel
import com.aatmik.calculator.Category.Category

class MainScreenViewModel : ViewModel() {

    fun onCategoryClick(category: Category) {
        when (category) {
            Category.BasicCalculator -> basicCalculator()
            Category.Age -> Age()
            Category.Bodies -> bodies()
            Category.BodyMassIndex -> bodyMassIndex()
            Category.Currency -> currency()
            Category.Equation -> equation()
            Category.Length -> length()
            Category.Percentage -> percentage()
            Category.Shape -> shape()
            Category.Speed -> speed()
            Category.Temperature -> temperature()
            Category.Tip -> tip()
            Category.Weight -> weight()
        }
    }

    private fun weight() {
        TODO("Not yet implemented")
    }

    private fun tip() {
        TODO("Not yet implemented")
    }

    private fun temperature() {
        TODO("Not yet implemented")
    }

    private fun speed() {
        TODO("Not yet implemented")
    }

    private fun shape() {
        TODO("Not yet implemented")
    }

    private fun percentage() {
        TODO("Not yet implemented")
    }

    private fun length() {
        TODO("Not yet implemented")
    }

    private fun equation() {
        TODO("Not yet implemented")
    }

    private fun currency() {
        TODO("Not yet implemented")
    }

    private fun bodyMassIndex() {
        TODO("Not yet implemented")
    }

    private fun bodies() {
        TODO("Not yet implemented")
    }

    private fun Age() {
        TODO("Not yet implemented")
    }

    private fun basicCalculator() {
        TODO("Not yet implemented")
    }


}