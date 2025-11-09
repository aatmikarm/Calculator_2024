package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentDiceRollerBinding
import com.aatmik.calculator.util.AnalyticsManager
import kotlin.random.Random

class DiceRollerFragment : Fragment() {

    private lateinit var binding: FragmentDiceRollerBinding

    // Statistics tracking
    private var totalRolls = 0
    private var rollHistory = mutableListOf<Int>()
    private var currentRollValue = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDiceRollerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prevent keyboard from affecting layout
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()
        setupClickListeners()

        // Initialize with default settings
        binding.diceImage.setImageResource(getDiceImageResource(1))
        binding.tvTotalSum.text = "1"
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
            rollButton.setOnClickListener {
                rollDice()
            }

            btnClearStats.setOnClickListener {
                clearStatistics()
            }
        }
    }

    private fun rollDice() {
        // Add some haptic feedback for better user experience
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

        // Animate dice rolling
        animateDiceRoll {
            val result = Random.nextInt(1, 7) // 1 to 6 for standard die
            currentRollValue = result
            binding.diceImage.setImageResource(getDiceImageResource(result))
            updateResults(result)
        }
    }

    private fun animateDiceRoll(onAnimationEnd: () -> Unit) {
        val diceView = binding.diceImage

        // Disable roll button during animation
        binding.rollButton.isEnabled = false

        // Create multiple animation effects
        val rotationAnimator = ObjectAnimator.ofFloat(diceView, "rotation", 0f, 720f) // 2 full rotations
        val scaleXAnimator = ObjectAnimator.ofFloat(diceView, "scaleX", 1f, 1.2f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(diceView, "scaleY", 1f, 1.2f, 1f)

        rotationAnimator.duration = 1200
        scaleXAnimator.duration = 1200
        scaleYAnimator.duration = 1200

        rotationAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleXAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleYAnimator.interpolator = AccelerateDecelerateInterpolator()

        // Fast image switching during rolling for realistic effect
        var imageChangeCounter = 0
        val imageChangeInterval = 80L // Change image every 80ms
        val maxImageChanges = 15 // Total number of image changes

        val imageChangeRunnable = object : Runnable {
            override fun run() {
                if (imageChangeCounter < maxImageChanges) {
                    val randomFace = Random.nextInt(1, 7)
                    diceView.setImageResource(getDiceImageResource(randomFace))
                    imageChangeCounter++
                    diceView.postDelayed(this, imageChangeInterval)
                } else {
                    // Animation finished, show final result
                    onAnimationEnd()
                    AnalyticsManager.logCalculationPerformed("Roll a Dice", "roll")
                    // Re-enable roll button
                    binding.rollButton.isEnabled = true
                }
            }
        }

        // Start all animations
        rotationAnimator.start()
        scaleXAnimator.start()
        scaleYAnimator.start()

        // Start image switching after a brief delay
        diceView.postDelayed(imageChangeRunnable, 100)
    }

    private fun updateResults(rollResult: Int) {
        binding.tvTotalSum.text = rollResult.toString()

        // Update statistics
        updateStatistics(rollResult)
    }

    private fun updateStatistics(rollResult: Int) {
        totalRolls++
        rollHistory.add(rollResult)

        val average = rollHistory.average()
        val highest = rollHistory.maxOrNull() ?: 0
        val lowest = rollHistory.minOrNull() ?: 0

        binding.apply {
            tvTotalRolls.text = totalRolls.toString()
            tvAverageRoll.text = String.format("%.1f", average)
            tvHighestRoll.text = highest.toString()
            tvLowestRoll.text = lowest.toString()

            // Show stats card after first roll
            if (totalRolls == 1) {
                statsCard.visibility = View.VISIBLE
            }
        }
    }

    private fun clearStatistics() {
        totalRolls = 0
        rollHistory.clear()
        currentRollValue = 1

        binding.apply {
            tvTotalRolls.text = "0"
            tvAverageRoll.text = "0.0"
            tvHighestRoll.text = "0"
            tvLowestRoll.text = "0"
            statsCard.visibility = View.GONE
            tvTotalSum.text = "1"
            diceImage.setImageResource(getDiceImageResource(1))
        }
    }

    private fun getDiceImageResource(value: Int): Int {
        return when (value) {
            1 -> R.drawable.dice_1  // Your PNG file for dice face 1
            2 -> R.drawable.dice_2  // Your PNG file for dice face 2
            3 -> R.drawable.dice_3  // Your PNG file for dice face 3
            4 -> R.drawable.dice_4  // Your PNG file for dice face 4
            5 -> R.drawable.dice_5  // Your PNG file for dice face 5
            6 -> R.drawable.dice_6  // Your PNG file for dice face 6
            else -> R.drawable.dice_1
        }
    }

    companion object {
        private const val TAG = "DiceRollerFragment"
    }
}