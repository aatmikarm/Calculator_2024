package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentPhCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.log10
import kotlin.math.pow

class PhCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentPhCalculatorBinding
    private var isCalculating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPhCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        animateView(binding.phLayout)
        hideKeyboardFunctionality(view)
        setupListeners()
        setupTextWatchers()
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculatePh()
            }

            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Quick calculation buttons
            btnPh.setOnClickListener {
                calculateSpecificValue("ph")
            }

            btnPoh.setOnClickListener {
                calculateSpecificValue("poh")
            }

            btnHydrogen.setOnClickListener {
                calculateSpecificValue("hydrogen")
            }

            btnHydroxide.setOnClickListener {
                calculateSpecificValue("hydroxide")
            }

            // Set up the editor action listeners
            etPh.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etPoh.requestFocus()
                    true
                } else {
                    false
                }
            }

            etPoh.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etHydrogenConcentration.requestFocus()
                    true
                } else {
                    false
                }
            }

            etHydrogenConcentration.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etHydroxideConcentration.requestFocus()
                    true
                } else {
                    false
                }
            }

            etHydroxideConcentration.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etTemperature.requestFocus()
                    true
                } else {
                    false
                }
            }

            etTemperature.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    autoCalculate()
                }
            }
        }

        binding.apply {
            etPh.addTextChangedListener(textWatcher)
            etPoh.addTextChangedListener(textWatcher)
            etHydrogenConcentration.addTextChangedListener(textWatcher)
            etHydroxideConcentration.addTextChangedListener(textWatcher)
            etTemperature.addTextChangedListener(textWatcher)
        }
    }

    private fun autoCalculate() {
        binding.apply {
            val ph = etPh.text.toString().toDoubleOrNull()
            val poh = etPoh.text.toString().toDoubleOrNull()
            val hConcentration = etHydrogenConcentration.text.toString().toDoubleOrNull()
            val ohConcentration = etHydroxideConcentration.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull() ?: 25.0

            val filledFields = listOfNotNull(ph, poh, hConcentration, ohConcentration).size

            // Auto-calculate if at least one field is filled
            if (filledFields >= 1) {
                isCalculating = true
                try {
                    calculatePhValues(temperature)
                } catch (e: Exception) {
                    // Handle calculation errors silently for auto-calculation
                }
                isCalculating = false
                updateResultDisplay()
            }
        }
    }

    private fun calculateSpecificValue(target: String) {
        binding.apply {
            val temperature = etTemperature.text.toString().toDoubleOrNull() ?: 25.0

            try {
                when (target) {
                    "ph" -> {
                        val hConcentration = etHydrogenConcentration.text.toString().toDoubleOrNull()
                        if (hConcentration != null && hConcentration > 0) {
                            val result = -log10(hConcentration)
                            etPh.setText(String.format("%.4f", result))
                        } else {
                            showError("Need hydrogen ion concentration to calculate pH")
                            return
                        }
                    }
                    "poh" -> {
                        val ohConcentration = etHydroxideConcentration.text.toString().toDoubleOrNull()
                        if (ohConcentration != null && ohConcentration > 0) {
                            val result = -log10(ohConcentration)
                            etPoh.setText(String.format("%.4f", result))
                        } else {
                            showError("Need hydroxide ion concentration to calculate pOH")
                            return
                        }
                    }
                    "hydrogen" -> {
                        val ph = etPh.text.toString().toDoubleOrNull()
                        if (ph != null) {
                            val result = 10.0.pow(-ph)
                            etHydrogenConcentration.setText(String.format("%.6e", result))
                        } else {
                            showError("Need pH to calculate hydrogen ion concentration")
                            return
                        }
                    }
                    "hydroxide" -> {
                        val poh = etPoh.text.toString().toDoubleOrNull()
                        if (poh != null) {
                            val result = 10.0.pow(-poh)
                            etHydroxideConcentration.setText(String.format("%.6e", result))
                        } else {
                            showError("Need pOH to calculate hydroxide ion concentration")
                            return
                        }
                    }
                }
                calculatePhValues(temperature)
                updateResultDisplay()
            } catch (e: Exception) {
                showError("Calculation error: ${e.message}")
            }
        }
    }

    private fun calculatePh() {
        binding.apply {
            val ph = etPh.text.toString().toDoubleOrNull()
            val poh = etPoh.text.toString().toDoubleOrNull()
            val hConcentration = etHydrogenConcentration.text.toString().toDoubleOrNull()
            val ohConcentration = etHydroxideConcentration.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull() ?: 25.0

            val filledFields = listOfNotNull(ph, poh, hConcentration, ohConcentration).size

            when {
                filledFields < 1 -> {
                    showError("Please enter at least one value")
                }
                else -> {
                    try {
                        calculatePhValues(temperature)
                        updateResultDisplay()
                        Toast.makeText(requireContext(), "pH calculation completed", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showError("Calculation error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun calculatePhValues(temperature: Double = 25.0) {
        binding.apply {
            var ph = etPh.text.toString().toDoubleOrNull()
            var poh = etPoh.text.toString().toDoubleOrNull()
            var hConcentration = etHydrogenConcentration.text.toString().toDoubleOrNull()
            var ohConcentration = etHydroxideConcentration.text.toString().toDoubleOrNull()

            // Temperature correction for Kw (simplified)
            val kw = when {
                temperature < 25 -> 1.0e-14 * (temperature / 25.0)
                temperature > 25 -> 1.0e-14 * (temperature / 25.0) * 1.5
                else -> 1.0e-14
            }

            val pKw = -log10(kw)

            // Calculate missing values based on available inputs
            when {
                ph != null -> {
                    // Calculate other values from pH
                    hConcentration = 10.0.pow(-ph)
                    poh = pKw - ph
                    ohConcentration = 10.0.pow(-poh)

                    if (etHydrogenConcentration.text.isEmpty())
                        etHydrogenConcentration.setText(String.format("%.6e", hConcentration))
                    if (etPoh.text.isEmpty())
                        etPoh.setText(String.format("%.4f", poh))
                    if (etHydroxideConcentration.text.isEmpty())
                        etHydroxideConcentration.setText(String.format("%.6e", ohConcentration))
                }

                poh != null -> {
                    // Calculate other values from pOH
                    ohConcentration = 10.0.pow(-poh)
                    ph = pKw - poh
                    hConcentration = 10.0.pow(-ph)

                    if (etPh.text.isEmpty())
                        etPh.setText(String.format("%.4f", ph))
                    if (etHydrogenConcentration.text.isEmpty())
                        etHydrogenConcentration.setText(String.format("%.6e", hConcentration))
                    if (etHydroxideConcentration.text.isEmpty())
                        etHydroxideConcentration.setText(String.format("%.6e", ohConcentration))
                }

                hConcentration != null && hConcentration > 0 -> {
                    // Calculate other values from [H+]
                    ph = -log10(hConcentration)
                    poh = pKw - ph
                    ohConcentration = kw / hConcentration

                    if (etPh.text.isEmpty())
                        etPh.setText(String.format("%.4f", ph))
                    if (etPoh.text.isEmpty())
                        etPoh.setText(String.format("%.4f", poh))
                    if (etHydroxideConcentration.text.isEmpty())
                        etHydroxideConcentration.setText(String.format("%.6e", ohConcentration))
                }

                ohConcentration != null && ohConcentration > 0 -> {
                    // Calculate other values from [OH-]
                    poh = -log10(ohConcentration)
                    ph = pKw - poh
                    hConcentration = kw / ohConcentration

                    if (etPh.text.isEmpty())
                        etPh.setText(String.format("%.4f", ph))
                    if (etPoh.text.isEmpty())
                        etPoh.setText(String.format("%.4f", poh))
                    if (etHydrogenConcentration.text.isEmpty())
                        etHydrogenConcentration.setText(String.format("%.6e", hConcentration))
                }
            }
        }
    }

    private fun getAcidityDescription(ph: Double): String {
        return when {
            ph < 0 -> "Extremely acidic (dangerous)"
            ph < 1 -> "Very strongly acidic"
            ph < 3 -> "Strongly acidic"
            ph < 5 -> "Moderately acidic"
            ph < 6 -> "Weakly acidic"
            ph < 7 -> "Slightly acidic"
            ph == 7.0 -> "Neutral"
            ph < 8 -> "Slightly basic"
            ph < 9 -> "Weakly basic"
            ph < 11 -> "Moderately basic"
            ph < 13 -> "Strongly basic"
            ph < 14 -> "Very strongly basic"
            else -> "Extremely basic (dangerous)"
        }
    }

    private fun updateResultDisplay() {
        binding.apply {
            val ph = etPh.text.toString().toDoubleOrNull()
            val poh = etPoh.text.toString().toDoubleOrNull()
            val hConcentration = etHydrogenConcentration.text.toString().toDoubleOrNull()
            val ohConcentration = etHydroxideConcentration.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull() ?: 25.0

            val result = StringBuilder()
            result.append("🧪 pH Calculation Results\n\n")

            // Show current values
            if (ph != null) {
                result.append("🔬 pH: %.4f\n".format(ph))
                result.append("📊 Solution type: ${getAcidityDescription(ph)}\n")
            }
            if (poh != null) {
                result.append("🔬 pOH: %.4f\n".format(poh))
            }
            if (hConcentration != null) {
                result.append("⚛️ [H⁺]: %.6e M\n".format(hConcentration))
            }
            if (ohConcentration != null) {
                result.append("⚛️ [OH⁻]: %.6e M\n".format(ohConcentration))
            }
            if (temperature != 25.0) {
                result.append("🌡️ Temperature: %.1f°C\n".format(temperature))
            }

            result.append("\n📝 Formulas Used:\n")
            result.append("• pH = -log[H⁺]\n")
            result.append("• pOH = -log[OH⁻]\n")
            result.append("• pH + pOH = 14 (at 25°C)\n")
            result.append("• [H⁺] × [OH⁻] = 1.0 × 10⁻¹⁴ (at 25°C)\n")
            result.append("• [H⁺] = 10⁻ᵖᴴ\n")
            result.append("• [OH⁻] = 10⁻ᵖᴼᴴ\n")

            if (ph != null) {
                result.append("\n💡 Common Examples:\n")
                when {
                    ph < 1 -> result.append("• Battery acid: ~0.5\n• Stomach acid: ~1.5-3.5\n")
                    ph < 3 -> result.append("• Lemon juice: ~2.0\n• Vinegar: ~2.4\n")
                    ph < 5 -> result.append("• Orange juice: ~3.3\n• Coffee: ~5.0\n")
                    ph < 7 -> result.append("• Milk: ~6.5\n• Saliva: ~6.5-7.5\n")
                    ph == 7.0 -> result.append("• Pure water: 7.0\n• Blood: ~7.4\n")
                    ph < 8 -> result.append("• Seawater: ~8.1\n• Baking soda: ~9.0\n")
                    ph < 10 -> result.append("• Hand soap: ~9.0-10.0\n")
                    ph < 12 -> result.append("• Household ammonia: ~11.0\n")
                    else -> result.append("• Household bleach: ~12.0\n• Lye: ~13.0\n")
                }

                // Buffer information
                if (ph >= 6.8 && ph <= 7.8) {
                    result.append("\n🩸 Biological Significance:\n")
                    result.append("• This pH range is crucial for biological systems\n")
                    result.append("• Human blood pH must stay between 7.35-7.45\n")
                    result.append("• Enzyme activity is pH-dependent\n")
                }

                // Safety warnings
                if (ph < 2 || ph > 12) {
                    result.append("\n⚠️ Safety Warning:\n")
                    result.append("• This pH level can be dangerous to handle\n")
                    result.append("• Use proper protective equipment\n")
                    result.append("• Can cause chemical burns\n")
                }
            }

            tvResult.text = result.toString()
        }
    }

    private fun clearAllFields() {
        binding.apply {
            etPh.text?.clear()
            etPoh.text?.clear()
            etHydrogenConcentration.text?.clear()
            etHydroxideConcentration.text?.clear()
            etTemperature.setText("25.0")
            tvResult.text = "Enter any pH-related value to calculate all others"
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.tvResult.text = "❌ Error: $message"
    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun hideKeyboardFunctionality(view: View) {
        // Set up the touch listener for non-text box views
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // Get the currently focused view (e.g., EditText)
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus() // Clear focus to remove cursor from EditText
                }
            }
            false
        }
    }

    private fun animateView(view: View) {
        // Use coroutine to stop the animation after 3 seconds
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            // Start the glow animation and store the animator reference
            glowAnimator = view.startGlowAnimation()
            delay(3000)  // Wait for 3 seconds
            view.stopGlowAnimation(glowAnimator)  // Stop the animation after delay
        }
    }

    companion object {
        private const val TAG = "PhCalculatorFragment"
    }
}