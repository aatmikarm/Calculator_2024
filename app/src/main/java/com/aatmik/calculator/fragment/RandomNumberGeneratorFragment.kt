package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentRandomNumberGeneratorBinding
import kotlin.random.Random

class RandomNumberGeneratorFragment : Fragment() {

    private lateinit var binding: FragmentRandomNumberGeneratorBinding
    private var isGenerating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRandomNumberGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prevent keyboard from affecting layout
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupUI() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            generateButton.setOnClickListener {
                generateRandomNumber()
            }

            // Quick preset buttons
            btnPreset1to10.setOnClickListener {
                setRange(1, 10)
            }

            btnPreset1to100.setOnClickListener {
                setRange(1, 100)
            }

            btnPreset1to1000.setOnClickListener {
                setRange(1, 1000)
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        binding.etMinNumber.addTextChangedListener(textWatcher)
        binding.etMaxNumber.addTextChangedListener(textWatcher)
    }

    private fun setRange(min: Int, max: Int) {
        binding.apply {
            etMinNumber.setText(min.toString())
            etMaxNumber.setText(max.toString())
        }
    }

    private fun validateInputs(): Boolean {
        val minText = binding.etMinNumber.text.toString()
        val maxText = binding.etMaxNumber.text.toString()

        if (minText.isEmpty() || maxText.isEmpty()) {
            return false
        }

        try {
            val minValue = minText.toLong()
            val maxValue = maxText.toLong()

            if (minValue >= maxValue) {
                showError("Minimum must be less than maximum")
                return false
            }

            if (maxValue - minValue > 1000000000L) {
                showError("Range too large. Please use a smaller range.")
                return false
            }

            return true
        } catch (e: NumberFormatException) {
            showError("Please enter valid numbers")
            return false
        }
    }

    private fun generateRandomNumber() {
        if (isGenerating) return

        if (!validateInputs()) {
            return
        }

        try {
            val minValue = binding.etMinNumber.text.toString().toLong()
            val maxValue = binding.etMaxNumber.text.toString().toLong()

            // Add haptic feedback
            addHapticFeedback()

            // Animate number generation
            animateNumberGeneration(minValue, maxValue)

        } catch (e: Exception) {
            showError("Error generating number: ${e.message}")
        }
    }

    private fun animateNumberGeneration(minValue: Long, maxValue: Long) {
        isGenerating = true
        binding.generateButton.isEnabled = false

        // Create spinning animation for the number display
        val displayView = binding.tvRandomNumber
        val rotationAnimator = ObjectAnimator.ofFloat(displayView, "rotation", 0f, 360f)
        rotationAnimator.duration = 800
        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()

        // Scale animation
        val scaleAnimator = ObjectAnimator.ofFloat(displayView, "scaleX", 1f, 1.3f, 1f)
        scaleAnimator.duration = 800
        val scaleYAnimator = ObjectAnimator.ofFloat(displayView, "scaleY", 1f, 1.3f, 1f)
        scaleYAnimator.duration = 800

        // Number rolling animation
        var rollCounter = 0
        val maxRolls = 20
        val rollInterval = 40L

        val numberRollRunnable = object : Runnable {
            override fun run() {
                if (rollCounter < maxRolls) {
                    val randomNumber = Random.nextLong(minValue, maxValue + 1)
                    displayView.text = randomNumber.toString()
                    rollCounter++
                    displayView.postDelayed(this, rollInterval)
                } else {
                    // Generate final number
                    val finalNumber = Random.nextLong(minValue, maxValue + 1)
                    displayView.text = finalNumber.toString()

                    // Re-enable button
                    binding.generateButton.isEnabled = true
                    isGenerating = false
                }
            }
        }

        // Start animations
        rotationAnimator.start()
        scaleAnimator.start()
        scaleYAnimator.start()

        // Start number rolling after short delay
        displayView.postDelayed(numberRollRunnable, 100)
    }

    private fun addHapticFeedback() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Vibration not available or permission denied
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "RandomNumberGeneratorFragment"
    }
}