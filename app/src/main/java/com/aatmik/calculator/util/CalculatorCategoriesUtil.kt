package com.aatmik.calculator.util

import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.model.Category

object CalculatorCategoriesUtil {

    // Define categories
    val categories = arrayListOf(
        Category("All"),
        // Category("Basic"),
        Category("Converters"),
        Category("Financial"),
        Category("Health & Fitness"),
        Category("Tools"),
        Category("Engineering"),
        Category("MBA"),
        Category("Physics"),
        Category("Math"),
        Category("Graphs"),
        Category("Biology"),
        Category("Chemistry"),
        Category("Probability"),
        Category("Music"),
        Category("Fun")
    )

    // Map calculators to their categories
    private val calculatorCategoryMap = mapOf(
        //"Basic" to "Basic",
        "Equation" to "Math",
        "Number Tables" to "Math",

        // Converters
        "Convertor" to "Converters",
        "Currency Converter" to "Converters",
        "Length" to "Converters",
        "Weight" to "Converters",
        "Speed" to "Converters",
        "Temperature" to "Converters",

        // Financial
        "Percentage" to "Financial",
        "Interest Calculator" to "Financial",
        "Loan Calculator" to "Financial",
        "Budget Calculator" to "Financial",
        "Tax Calculator" to "Financial",
        "Contribution Calculator" to "Financial",
        "Tip" to "Financial",
        "Ratio Calculator" to "Financial",

        // Health & Fitness
        "Body Mass Index" to "Health & Fitness",
        "Calorie Calculator" to "Health & Fitness",
        "Age" to "Health & Fitness",
        "Pregnancy Calculator" to "Health & Fitness",
        "Period Calculator" to "Health & Fitness",
        "Ovulation & Fertility" to "Health & Fitness",

        // Tools
        "Stopwatch" to "Tools",
        "Compass" to "Tools",
        "Level" to "Tools",

        // Physics
        "Free Fall" to "Physics",
        "Lightning Calculator" to "Physics",
        "Ohm's Law" to "Physics",
        "Fuel Economy Calculator" to "Physics",
        "Trip Estimate" to "Physics",

        // Math
        "Area Calculator" to "Math",
        "Ratio Calculator" to "Math",
        "Fraction Calculator" to "Math",
        "Proportion Calculator" to "Math",
        "LCM & GCD Calculator" to "Math",
        "GPA Calculator" to "Math",
        "Shapes" to "Math",
        "Bodies" to "Math",

        // Chemistry
        // Add your chemistry calculators here when ready
         "Molarity Calculator" to "Chemistry",
         "pH Calculator" to "Chemistry",
         "Stoichiometry Calculator" to "Chemistry",
         "Ideal Gas Law" to "Chemistry",

        // Fun
        "Love Calculator" to "Fun",
        "Friendship Calculator" to "Fun"
    )

    /**
     * Get calculators for a specific category
     */
    fun getCalculatorsForCategory(category: String, allCalculators: ArrayList<Calculator>): ArrayList<Calculator> {
        return if (category == "All") {
            allCalculators
        } else {
            ArrayList(allCalculators.filter { calculator ->
                calculatorCategoryMap[calculator.name] == category
            })
        }
    }

    /**
     * Get category name for a specific calculator
     */
    fun getCategoryForCalculator(calculatorName: String): String {
        return calculatorCategoryMap[calculatorName] ?: "Basic"
    }

    /**
     * Filter calculators by search query within a category
     */
    fun filterCalculatorsBySearch(
        searchQuery: String,
        category: String,
        allCalculators: ArrayList<Calculator>
    ): ArrayList<Calculator> {
        val categoryCalculators = getCalculatorsForCategory(category, allCalculators)

        return if (searchQuery.isEmpty()) {
            categoryCalculators
        } else {
            ArrayList(categoryCalculators.filter { calculator ->
                calculator.name.lowercase().contains(searchQuery.lowercase())
            })
        }
    }
}