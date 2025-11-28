package com.aatmik.calculator.util

import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.model.Category

object CalculatorCategoriesUtil {

    // Define categories with icons and descriptions
    val categories = arrayListOf(
        Category("All", R.drawable.calculator_new, "All calculators"),
        Category("Converters", R.drawable.convert, "Unit conversions"),
        Category("Financial", R.drawable.loan, "Money & finance"),
        Category("Health & Fitness", R.drawable.bmi, "Body & wellness"),
        Category("Utility & Tools", R.drawable.stopwatch, "Utility & Tools"),
        Category("Engineering", R.drawable.engineering, "Engineering calcs"),
        Category("MBA", R.drawable.mba, "Business analysis"),
        Category("Physics", R.drawable.physics, "Physics formulas"),
        Category("Math", R.drawable.equation, "Math operations"),
        Category("Graphs", R.drawable.graphs_bar, "Visual graphs"),
        Category("Biology", R.drawable.biology, "Life sciences"),
        Category("Chemistry", R.drawable.chemistry, "Chemical calcs"),
        Category("Probability", R.drawable.probability, "Random & stats"),
        Category("Music", R.drawable.music, "Music theory"),
        Category("Fun", R.drawable.love, "Fun calculators")
    )

    // Map calculators to their categories
    private val calculatorCategoryMap = mapOf(
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
        "FD Calculator" to "Financial",
        "Interest Calculator" to "Financial",
        "Loan Calculator" to "Financial",
        "ROI Calculator" to "Financial",
        "Investment Calculator" to "Financial",
        "GST Calculator" to "Financial",
        "Loan Comparison" to "Financial",
        "Budget Calculator" to "Financial",
        "Tax Calculator" to "Financial",
        "Contribution Calculator" to "Financial",
        "Tip" to "Financial",
        "Ratio Calculator" to "Financial",

        // MBA
        "Break-Even Analysis" to "MBA",
        "NPV & IRR Calculator" to "MBA",

        // Engineering
        "Beam Calculator" to "Engineering",
        "Column Buckling" to "Engineering",
        "Voltage Divider" to "Engineering",

        // Graphs
        "Function Grapher" to "Graphs",
        "Statistical Graph Generator" to "Graphs",
        "Coordinate Geometry" to "Graphs",

        // Health & Fitness
        "Body Mass Index" to "Health & Fitness",
        "Calorie Calculator" to "Health & Fitness",
        "Age" to "Health & Fitness",
        "Pregnancy Calculator" to "Health & Fitness",
        "Period Calculator" to "Health & Fitness",
        "Ovulation & Fertility" to "Health & Fitness",

        // Tools
        "Stopwatch" to "Utility & Tools",
        "Compass" to "Utility & Tools",
        "Level" to "Utility & Tools",

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

        // Biology
        "Punnett Square" to "Biology",
        "Cell Dilution" to "Biology",

        // Chemistry
        "Molarity Calculator" to "Chemistry",
        "pH Calculator" to "Chemistry",
        "Stoichiometry Calculator" to "Chemistry",
        "Ideal Gas Law" to "Chemistry",

        // Fun
        "Love Calculator" to "Fun",
        "Friendship Calculator" to "Fun",
        "Roll a Dice" to "Fun",
        "Random Number Generator" to "Fun"
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