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
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
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
    var isSecondMode = false
    var isInDegreesMode = true

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

                    // Disable and fade out the DEG/RAD button
                    btDeg.isEnabled = false
                    btDeg.alpha = 0.5f // Set alpha to fade it out (make it look dull)
                } else {
                    // Change back to normal functions
                    btSin.text = "sin"
                    btCos.text = "cos"
                    btTan.text = "tan"

                    // Enable and bring back the full opacity of the DEG/RAD button
                    btDeg.isEnabled = true
                    btDeg.alpha = 1.0f // Restore full opacity
                }
            }

            btSin.setOnClickListener {
                vibratePhone(requireContext())
                if (isSecondMode) {
                    onScientificFunctionClicked("asin") // Inverse sine
                } else {
                    onScientificFunctionClicked("sin")  // Regular sine
                }
            }
            btCos.setOnClickListener {
                vibratePhone(requireContext())
                if (isSecondMode) {
                    onScientificFunctionClicked("acos") // Inverse cosine
                } else {
                    onScientificFunctionClicked("cos")  // Regular cosine
                }
            }
            btTan.setOnClickListener {
                vibratePhone(requireContext())
                if (isSecondMode) {
                    onScientificFunctionClicked("atan") // Inverse tangent
                } else {
                    onScientificFunctionClicked("tan")  // Regular tangent
                }
            }
            btDeg.setOnClickListener {
                vibratePhone(requireContext())

                // Toggle between degrees and radians mode
                isInDegreesMode = !isInDegreesMode

                if (!isInDegreesMode) { // Radian mode selected
                    btDeg.text = "rad" // Update the button text
                    disableSecondButton() // Disable and fade out the 2nd button
                } else { // Degree mode selected
                    btDeg.text = "deg" // Update the button text
                    enableSecondButton() // Enable and restore the 2nd button
                }
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

    // Function to disable and fade out the 2nd button
    fun disableSecondButton() {
        binding.btSecond.isEnabled = false // Disable the button
        binding.btSecond.alpha = 0.5f // Set opacity to make it look faded
    }

    // Function to enable and restore the 2nd button
    fun enableSecondButton() {
        binding.btSecond.isEnabled = true // Enable the button
        binding.btSecond.alpha = 1.0f // Restore full opacity
    }

    private fun onScientificFunctionClicked(function: String) {
        val currentInput = binding.tvPrimaryBC.text.toString().toDoubleOrNull()

        if (currentInput == null) {
            Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            return
        }

        val result: Double = when (function) {
            "sin" -> if (isInDegreesMode) sin(Math.toRadians(currentInput)) else sin(currentInput)
            "cos" -> if (isInDegreesMode) cos(Math.toRadians(currentInput)) else cos(currentInput)
            "tan" -> if (isInDegreesMode) tan(Math.toRadians(currentInput)) else tan(currentInput)
            "asin" -> Math.toDegrees(asin(currentInput)) // Always return in degrees
            "acos" -> Math.toDegrees(acos(currentInput)) // Always return in degrees
            "atan" -> Math.toDegrees(atan(currentInput)) // Always return in degrees
            "sqrt" -> sqrt(currentInput)
            "lg" -> log10(currentInput)
            "ln" -> ln(currentInput)
            "factorial" -> factorial(currentInput.toInt())
            "inverse" -> 1 / currentInput
            "pi" -> currentInput * PI
            else -> currentInput
        }

        // Format the result to 6 decimal places
        val formattedResult = String.format("%.9f", result)

        // Check if we are in inverse mode and append degree symbol accordingly
        val displayResult =
            if (isSecondMode && (function == "asin" || function == "acos" || function == "atan")) {
                "$formattedResult°" // Append degree symbol in inverse mode
            } else {
                formattedResult // Display the result as it is for other functions
            }

        // Update the input text view with the result
        binding.tvPrimaryBC.text = displayResult
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