package com.example.yourapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentMathEquationSolverBinding

class MathEquationSolverFragment : Fragment() {

    private var _binding: FragmentMathEquationSolverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMathEquationSolverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.solveButton.setOnClickListener {
            hideKeyboard()  // Hide the keyboard
            solveQuadraticEquation()
        }

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun solveQuadraticEquation() {
        val a = binding.coefficientA.text.toString().toDoubleOrNull()
        val b = binding.coefficientB.text.toString().toDoubleOrNull()
        val c = binding.coefficientC.text.toString().toDoubleOrNull()

        if (a == null || b == null || c == null) {
            binding.resultTextView.text = "Please enter valid coefficients!"
            return
        }

        val discriminant = b * b - 4 * a * c
        val resultText = StringBuilder()

        if (discriminant > 0) {
            val root1 = (-b + Math.sqrt(discriminant)) / (2 * a)
            val root2 = (-b - Math.sqrt(discriminant)) / (2 * a)
            resultText.append("Roots: x₁ = %.2f, x₂ = %.2f".format(root1, root2))
        } else if (discriminant == 0.0) {
            val root = -b / (2 * a)
            resultText.append("One root: x = %.2f".format(root))
        } else {
            resultText.append("No real roots")
        }

        // Show the equation steps
        val stepsText = "Equation: $a x² + $b x + $c = 0\n" +
                "Discriminant: D = b² - 4ac = %.2f".format(discriminant)

        binding.resultTextView.text = resultText.toString()
        binding.stepsTextView.text = stepsText
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = activity?.currentFocus ?: View(context)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
