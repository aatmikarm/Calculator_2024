package com.aatmik.calculator.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentLoveCalculatorBinding
import kotlin.math.abs
import kotlin.random.Random

class LoveCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentLoveCalculatorBinding
    private var currentScore = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoveCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupClickListeners()
        setupTextWatchers()
        updateCalculateButtonState()
    }

    private fun setupClickListeners() {
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnCalculate.setOnClickListener {
            calculateLove()
        }

        binding.btnClear.setOnClickListener {
            clearFields()
        }

        binding.btnSwapNames.setOnClickListener {
            swapNames()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCalculateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etFirstName.addTextChangedListener(textWatcher)
        binding.etSecondName.addTextChangedListener(textWatcher)
    }

    private fun updateCalculateButtonState() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        binding.btnCalculate.isEnabled = firstName.isNotEmpty() && secondName.isNotEmpty()
        binding.btnCalculate.alpha = if (binding.btnCalculate.isEnabled) 1.0f else 0.5f
    }

    private fun calculateLove() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        if (firstName.isEmpty() || secondName.isEmpty()) {
            return
        }

        // Hide keyboard
        hideKeyboard()

        // Show result layout and start calculation
        binding.resultLayout.visibility = View.VISIBLE
        binding.loveResultCard.visibility = View.VISIBLE

        // Calculate love score
        val score = calculateLoveScore(firstName, secondName)

        // Animate the score
        animateScore(score)

        // Update result text and colors
        updateResultDisplay(score, firstName, secondName)
    }

    private fun calculateLoveScore(name1: String, name2: String): Int {
        // Create a deterministic but seemingly random algorithm
        val combinedNames = (name1 + name2).lowercase()

        // Count letter frequencies for "LOVE"
        val loveLetters = "love"
        var loveCount = 0

        for (char in combinedNames) {
            if (char in loveLetters) {
                loveCount++
            }
        }

        // Use a combination of name lengths, letter frequencies, and hash codes
        val lengthFactor = (name1.length + name2.length) % 10
        val hashFactor = abs((name1 + name2).hashCode()) % 100
        val loveFactor = loveCount * 7

        // Create a score between 1-100
        var score = ((hashFactor + lengthFactor + loveFactor) % 100)

        // Ensure minimum score of 1
        if (score == 0) score = 1

        return score
    }

    private fun animateScore(targetScore: Int) {
        val animator = ValueAnimator.ofInt(0, targetScore)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            currentScore = animation.animatedValue as Int
            binding.tvLoveScore.text = "$currentScore%"
            updateProgressBar(currentScore)
        }

        animator.start()
    }

    private fun updateProgressBar(score: Int) {
        binding.progressBar.progress = score

        // Change progress bar color based on score
        val colorRes = when {
            score >= 80 -> R.color.love_excellent
            score >= 60 -> R.color.love_good
            score >= 40 -> R.color.love_average
            else -> R.color.love_poor
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), colorRes)
    }

    private fun updateResultDisplay(score: Int, firstName: String, secondName: String) {
        // Set names
        binding.tvNameResult.text = "$firstName ❤️ $secondName"

        // Set compatibility message and icon
        val (message, emoji, colorRes) = when {
            score >= 90 -> Triple("Perfect Match! You're soulmates!", "💕", R.color.love_excellent)
            score >= 80 -> Triple("Excellent compatibility! True love!", "❤️", R.color.love_excellent)
            score >= 70 -> Triple("Very good match! Strong connection!", "💖", R.color.love_good)
            score >= 60 -> Triple("Good compatibility! Great potential!", "💗", R.color.love_good)
            score >= 50 -> Triple("Average match. Work on it!", "💛", R.color.love_average)
            score >= 40 -> Triple("Below average. Need more effort!", "💙", R.color.love_average)
            score >= 30 -> Triple("Low compatibility. Challenge ahead!", "💜", R.color.love_poor)
            else -> Triple("Very low match. Better as friends?", "💔", R.color.love_poor)
        }

        binding.tvCompatibilityMessage.text = message
        binding.tvLoveEmoji.text = emoji

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.tvLoveScore.setTextColor(color)
        binding.tvCompatibilityMessage.setTextColor(color)
    }

    private fun clearFields() {
        binding.etFirstName.text?.clear()
        binding.etSecondName.text?.clear()
        binding.resultLayout.visibility = View.GONE
        binding.loveResultCard.visibility = View.GONE
        currentScore = 0

        // Reset focus to first name field
        binding.etFirstName.requestFocus()
    }

    private fun swapNames() {
        val firstName = binding.etFirstName.text.toString()
        val secondName = binding.etSecondName.text.toString()

        binding.etFirstName.setText(secondName)
        binding.etSecondName.setText(firstName)

        // If result is visible, recalculate
        if (binding.resultLayout.visibility == View.VISIBLE) {
            calculateLove()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    companion object {
        private const val TAG = "LoveCalculatorFragment"
    }
}