package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentPercentageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation

class PercentageFragment : Fragment() {

    private lateinit var binding: FragmentPercentageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPercentageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animateView(binding.percentageLayout)

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {

                hideKeyboard()
                // Get the input values from EditTexts
                calculatePercentage()

            }
            // Set an OnClickListener on the CardView
            swap.setOnClickListener {
                // Check if either value is empty

                var originalAmountStr = etOriginalAmount.text.toString()
                var percentageStr = etPercentage.text.toString()

                if (originalAmountStr.isEmpty() || percentageStr.isEmpty()) {
                    Toast.makeText(
                        requireContext(), "Both values must be non-empty!", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Swap values
                    val temp = originalAmountStr
                    originalAmountStr = percentageStr
                    percentageStr = temp

                    etOriginalAmount.setText(originalAmountStr)
                    etPercentage.setText(percentageStr)

                    calculatePercentage()
                }
            }

            etPercentage.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // When the user stops typing, this is called
                    calculatePercentage()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // While the text is being changed
                    calculatePercentage()
                }
            })

            etOriginalAmount.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // When the user stops typing, this is called
                    calculatePercentage()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // While the text is being changed
                    calculatePercentage()
                }
            })

            // Set up the editor action listener to handle "Done" action
            etOriginalAmount.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etPercentage.requestFocus() // Move focus to etPercentage
                    true // Indicate that the event was handled
                } else {
                    false // Not handled
                }
            }

        }
    }


    private fun animateView(view: View) {
        // Use coroutine to stop the animation after 5 seconds
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            // Start the glow animation and store the animator reference
            glowAnimator = view.startGlowAnimation()
            delay(3000)  // Wait for 5 seconds
            view.stopGlowAnimation(glowAnimator)  // Stop the animation after delay
        }
    }

    private fun calculatePercentage() {
        binding.apply {
            val originalAmountStr = etOriginalAmount.text.toString()
            val percentageStr = etPercentage.text.toString()

            // Check if inputs are valid (not empty)
            if (originalAmountStr.isNotEmpty() && percentageStr.isNotEmpty()) {
                val originalAmount = originalAmountStr.toDouble()
                val percentage = percentageStr.toDouble()

                // Calculated percentage of the original amount
                val percentageOfAmount = (originalAmount * percentage) / 100

                // Remaining amount after subtracting percentage
                val remainingAmount = originalAmount - percentageOfAmount

                // Original amount after adding the percentage
                val amountWithAddedPercentage = originalAmount + percentageOfAmount

                // Fraction representation of percentage
                val fractionRepresentation = "1/${100 / percentage}"

                // Multiplication factor (percentage divided by 100)
                val multiplicationFactor = percentage / 100

                // Display all the results in TextViews
                tvPercentageValue.text = "$percentageOfAmount"
                tvRemainingValue.text = "$remainingAmount"
                tvAmountWithAddedValue.text = "$amountWithAddedPercentage"
                tvFractionValue.text = "$fractionRepresentation"
                tvMultiplicationValue.text = "$multiplicationFactor"
            } else {
                // Show error if inputs are invalid
                // Toast.makeText(context, "Please enter both fields.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    companion object {
        private const val TAG = "PercentageFragment"
    }
}