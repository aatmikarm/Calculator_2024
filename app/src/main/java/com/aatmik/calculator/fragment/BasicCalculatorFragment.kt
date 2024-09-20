package com.aatmik.calculator.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentBasicCalculatorBinding
import com.aatmik.calculator.util.ButtonUtil.addNumberValueToText
import com.aatmik.calculator.util.ButtonUtil.addOperatorValueToText
import com.aatmik.calculator.util.ButtonUtil.invalidInputToast
import com.aatmik.calculator.util.ButtonUtil.vibratePhone
import com.aatmik.calculator.util.CalculationUtil
import com.aatmik.calculator.util.PrefUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan


class BasicCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentBasicCalculatorBinding
    private var isPanelVisible = false // Track visibility state of the panel

    companion object {
        var addedBC = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBasicCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleBarLogic()
        setupBasicButtons()
        setupScientificButtons()

    }

    private fun setupBasicButtons() {
        binding.apply {
            addNumberValueToText(requireContext(), bt0BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt1BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt2BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt3BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt4BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt5BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt6BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt7BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt8BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), bt9BC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), btBracketOpenBC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), btBracketCloseBC, tvPrimaryBC, 0)
            addOperatorValueToText(requireContext(), btAdditionBC, tvPrimaryBC, "+", 0)
            addOperatorValueToText(requireContext(), btSubtractionBC, tvPrimaryBC, "-", 0)
            addOperatorValueToText(requireContext(), btMultiplicationBC, tvPrimaryBC, "*", 0)
            addOperatorValueToText(requireContext(), btDivisionBC, tvPrimaryBC, "/", 0)
            btDotBC.setOnClickListener {
                vibratePhone(requireContext())
                if (!tvPrimaryBC.text.contains(".")) tvPrimaryBC.text =
                    tvPrimaryBC.text.toString() + "."
            }
            btACBC.setOnClickListener {
                vibratePhone(requireContext())

                tvPrimaryBC.text = ""
                tvSecondaryBC.text = ""
                addedBC = false
            }
            btDeleteBC.setOnClickListener {
                vibratePhone(requireContext())

                if (tvPrimaryBC.text.contains("+") || tvPrimaryBC.text.contains("-") || tvPrimaryBC.text.contains(
                        "*"
                    ) || tvPrimaryBC.text.contains("/")
                ) addedBC = false

                if (tvPrimaryBC.text.isNotEmpty()) tvPrimaryBC.text =
                    tvPrimaryBC.text.subSequence(0, tvPrimaryBC.length() - 1)
            }
            btEqualBC.setOnClickListener {

                vibratePhone(requireContext())

                if (isPowerMode && baseValue != null) {
                    // The user is in power mode and has entered the exponent
                    val exponentInput = binding.tvPrimaryBC.text.toString().split("^").lastOrNull()
                        ?.toDoubleOrNull()

                    if (exponentInput != null) {
                        // Perform x^y calculation
                        val result = Math.pow(baseValue!!, exponentInput)
                        binding.tvPrimaryBC.text = result.toString()
                        binding.tvSecondaryBC.text = "${baseValue}^$exponentInput = $result"
                        isPowerMode = false // Reset power mode
                        baseValue = null // Clear the base value
                    } else {
                        invalidInputToast(requireContext())
                    }
                } else {
                    // Regular evaluation
                    try {
                        if (tvPrimaryBC.text.isNotEmpty()) {
                            val input = tvPrimaryBC.text.toString()
                            val result = CalculationUtil.evaluate(input).toString()
                            tvPrimaryBC.text = CalculationUtil.trimResult(result)
                            tvSecondaryBC.text = input
                            addedBC = false
                        }
                    } catch (e: Exception) {
                        invalidInputToast(requireContext())
                    }
                }
            }
        }

    }

    private var isPowerMode = false
    private var baseValue: Double? = null

    // Variables to track the state of inverse functions
    var isSecondMode = false
    private fun setupScientificButtons() {
        binding.apply {
            // Adding vibration to each button click

            btSecond.setOnClickListener {
                vibratePhone(requireContext())
                isSecondMode = !isSecondMode

                if (isSecondMode) {
                    // Change to inverse functions
                    btSin.text = "sin⁻¹"
                    btCos.text = "cos⁻¹"
                    btTan.text = "tan⁻¹"
                    btDeg.isEnabled = false // Disable deg button
                } else {
                    // Change back to normal functions
                    btSin.text = "sin"
                    btCos.text = "cos"
                    btTan.text = "tan"
                    btDeg.isEnabled = true // Enable deg button
                }
            }

            btSin.setOnClickListener {
                onScientificFunctionClicked("sin")
                vibratePhone(requireContext())
            }
            btCos.setOnClickListener {
                onScientificFunctionClicked("cos")
                vibratePhone(requireContext())
            }
            btTan.setOnClickListener {
                onScientificFunctionClicked("tan")
                vibratePhone(requireContext())
            }
            btDeg.setOnClickListener {
                onScientificFunctionClicked("deg")
                vibratePhone(requireContext())  // Call vibrate function
            }
            btRootX.setOnClickListener {
                onScientificFunctionClicked("sqrt")
                vibratePhone(requireContext())  // Call vibrate function
            }


            btPowerXY.setOnClickListener {
                vibratePhone(requireContext())

                // Get the current input as the base value for x^y
                val currentInput = binding.tvPrimaryBC.text.toString().toDoubleOrNull()

                if (currentInput != null) {
                    baseValue = currentInput
                    binding.tvPrimaryBC.text =
                        "$currentInput^" // Show "2^" when user clicks the button
                    isPowerMode = true  // Set power mode to true
                } else {
                    Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }

            btLg.setOnClickListener {
                onScientificFunctionClicked("lg")
                vibratePhone(requireContext())  // Call vibrate function
            }
            btLn.setOnClickListener {
                onScientificFunctionClicked("ln")
                vibratePhone(requireContext())  // Call vibrate function
            }
            btFactorial.setOnClickListener {
                onScientificFunctionClicked("factorial")
                vibratePhone(requireContext())  // Call vibrate function
            }
            btInverse.setOnClickListener {
                onScientificFunctionClicked("inverse")
                vibratePhone(requireContext())  // Call vibrate function
            }
            btPi.setOnClickListener {
                onScientificFunctionClicked("pi")
                vibratePhone(requireContext())  // Call vibrate function
            }
        }
    }


    private fun onScientificFunctionClicked(function: String) {
        val currentInput = binding.tvPrimaryBC.text.toString().toDoubleOrNull()

        if (currentInput == null) {
            Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            return
        }

        val result: Double = when (function) {
            "sin" -> sin(Math.toRadians(currentInput))
            "cos" -> cos(Math.toRadians(currentInput))
            "tan" -> tan(Math.toRadians(currentInput))
            "deg" -> Math.toDegrees(currentInput)
            "sqrt" -> sqrt(currentInput)
            "lg" -> log10(currentInput)
            "ln" -> ln(currentInput)
            "factorial" -> factorial(currentInput.toInt())
            "inverse" -> 1 / currentInput
            "pi" -> currentInput * PI
            else -> currentInput
        }

        // Update the input text view with the result
        binding.tvPrimaryBC.text = result.toString()
    }

    // Factorial function for integers
    private fun factorial(n: Int): Double {
        return try {
            if (n < 0) {
                Toast.makeText(
                    context,
                    "Factorial is not defined for negative numbers",
                    Toast.LENGTH_SHORT
                ).show()
                1.0
            } else if (n > 170) {
                Toast.makeText(
                    context,
                    "Number too large for factorial calculation",
                    Toast.LENGTH_SHORT
                ).show()
                Double.POSITIVE_INFINITY
            } else {
                if (n == 0 || n == 1) 1.0 else n * factorial(n - 1)
            }
        } catch (e: ArithmeticException) {
            Toast.makeText(context, "Overflow error occurred", Toast.LENGTH_SHORT).show()
            Double.POSITIVE_INFINITY
        }
    }

    private fun toggleBarLogic() {
        // Toggle the scientific buttons panel when the toggle bar is clicked
        binding.toggleBar.setOnClickListener {
            if (isPanelVisible) {
                slideDown(binding.scientificButtonsPanel)
                binding.toggleArrow.rotation = 0f // Rotate arrow to point down
            } else {
                slideUp(binding.scientificButtonsPanel)
                binding.toggleArrow.rotation = 180f // Rotate arrow to point up
            }
            isPanelVisible = !isPanelVisible
        }
    }

    // Slide up function to show the scientific buttons
    private fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    // Slide down function to hide the scientific buttons
    private fun slideDown(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat())
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        animator.addListener(object : android.animation.Animator.AnimatorListener {

            override fun onAnimationStart(p0: Animator) {
                TODO("Not yet implemented")
            }

            override fun onAnimationEnd(p0: Animator) {
                view.visibility = View.GONE
            }

            override fun onAnimationCancel(p0: Animator) {
                TODO("Not yet implemented")
            }

            override fun onAnimationRepeat(p0: Animator) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            tvPrimaryBC.text = PrefUtil.getPrimaryTextBC(requireContext())
            tvSecondaryBC.text = PrefUtil.getSecondaryTextBC(requireContext())
        }

    }

    override fun onStop() {
        super.onStop()
        binding.apply {
            PrefUtil.setPrimaryTextBC(requireContext(), tvPrimaryBC.text.toString())
            PrefUtil.setSecondaryTextBC(requireContext(), tvSecondaryBC.text.toString())
        }

    }
}