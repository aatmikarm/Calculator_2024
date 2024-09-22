package com.aatmik.calculator.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentPercentageBinding

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
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {

                hideKeyboard()
                // Get the input values from EditTexts
                val originalAmountStr = etOriginalAmount.text.toString()
                val percentageStr = etPercentage.text.toString()

                // Check if inputs are valid (not empty)
                if (originalAmountStr.isNotEmpty() && percentageStr.isNotEmpty()) {
                    // Parse input to Double
                    val originalAmount = originalAmountStr.toDouble()
                    val percentage = percentageStr.toDouble()

                    // Calculate percentage
                    val result = (originalAmount * percentage) / 100

                    // Display result
                    tvResult.text = "Result: $result"
                } else {
                    // Show error if inputs are invalid
                    tvResult.text = "Please enter both fields."
                }
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