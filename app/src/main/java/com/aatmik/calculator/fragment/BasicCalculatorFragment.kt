package com.aatmik.calculator.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.HistoryAdapter
import com.aatmik.calculator.adapter.HistoryBottomSheetAdapter
import com.aatmik.calculator.databinding.FragmentBasicCalculatorBinding
import com.aatmik.calculator.model.CalculationHistory
import com.aatmik.calculator.util.AnalyticsManager
import com.aatmik.calculator.util.ButtonUtil
import com.aatmik.calculator.util.ButtonUtil.addNumberValueToText
import com.aatmik.calculator.util.ButtonUtil.addOperatorValueToText
import com.aatmik.calculator.util.ButtonUtil.invalidInputToast
import com.aatmik.calculator.util.ButtonUtil.vibratePhone
import com.aatmik.calculator.util.CalculationUtil
import com.aatmik.calculator.util.HistoryManager
import com.aatmik.calculator.util.PrefUtil
import com.aatmik.calculator.util.SwipeGestureListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

class BasicCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentBasicCalculatorBinding
    private var isPanelVisible = false
    private val calculationHistory = mutableListOf<CalculationHistory>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView

    private var historyBottomSheet: BottomSheetDialog? = null
    private var historyOverlayView: View? = null
    private var isHistoryVisible = false

    // Advanced calculator states
    private var isPowerMode = false
    private var baseValue: Double? = null
    private var isSecondMode = false
    private var isInDegreesMode = true

    // Memory functionality
    private var memoryValue: Double = 0.0

    // Undo/Redo functionality
    private val expressionHistory = mutableListOf<String>()
    private var historyIndex = -1

    // Input validation
    private val inputHandler = Handler(Looper.getMainLooper())
    private var inputRunnable: Runnable? = null

    // Precision handling
    private val mathContext = MathContext(34, RoundingMode.HALF_UP)

    companion object {
        var addedBC = false
    }

    // Validation result enum
    enum class ValidationResult(val message: String) {
        VALID("Valid"),
        EMPTY("Empty expression"),
        CONSECUTIVE_OPERATORS("Consecutive operators"),
        UNBALANCED_PARENTHESES("Unbalanced parentheses"),
        INCOMPLETE("Incomplete expression"),
        INVALID_CHARS("Invalid characters")
    }

    // Calculation result sealed class
    sealed class CalculationResult {
        data class Success(val value: Double) : CalculationResult()
        data class Error(val message: String) : CalculationResult()
    }

    // Error types
    enum class ErrorType {
        CALCULATION, SYNTAX, OVERFLOW, DOMAIN
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBasicCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()
        setupButtons()
        historyView()
        restoreMemoryState()
        setupSwipeGesture()
    }

    private fun setupSwipeGesture() {
        val swipeListener = SwipeGestureListener(requireContext()) {
            if (!isHistoryVisible) {
                showHistoryFromTop()
            }
        }
        binding.root.setOnTouchListener(swipeListener)
    }

    private fun setupUI() {
        toggleBarLogic()
        // Clear any error states on start
        clearError()
    }

    private fun setupButtons() {
        setupBasicButtons()
        setupScientificButtons()
        setupAdvancedButtons()
        setupMemoryButtons()
        setupControlButtons()
    }

    private fun historyView() {
        recyclerView = binding.rvHistory
        historyAdapter = HistoryAdapter(calculationHistory) { historyItem ->
            addToExpressionHistory(binding.tvPrimaryBC.text.toString())
            binding.tvPrimaryBC.text = historyItem.result
            validateAndUpdateUI()
            AnalyticsManager.log("calculation_history_used")
        }

        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        binding.btHistory.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            if (isHistoryVisible) {
                hideHistoryFromTop()
            } else {
                showHistoryFromTop()
            }
        }
    }

    // New method: Show history with top-down animation
    private fun showHistoryFromTop() {
        if (isHistoryVisible) return

        val parentView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        val historyView = layoutInflater.inflate(R.layout.bottom_sheet_history, parentView, false)

        // Setup views
        val rvHistoryList = historyView.findViewById<RecyclerView>(R.id.rvHistoryList)
        val emptyStateLayout = historyView.findViewById<LinearLayout>(R.id.emptyStateLayout)
        val btnClearHistory = historyView.findViewById<ImageView>(R.id.btnClearHistory)
        val btnCloseHistory = historyView.findViewById<ImageView>(R.id.btnCloseHistory)

        // Get history data
        val historyList = HistoryManager.getHistory(requireContext())

        // Show/hide empty state
        if (historyList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            rvHistoryList.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            rvHistoryList.visibility = View.VISIBLE
        }

        // Setup adapter with fast animation
        val adapter = HistoryBottomSheetAdapter(
            historyList,
            onReuse = { result ->
                // Fast population animation
                animateResultPopulation(result)
                hideHistoryFromTop()
            },
            onDelete = { position ->
                showDeleteConfirmationDialog(position) {
                    refreshHistoryInOverlay(rvHistoryList, emptyStateLayout)
                }
            }
        )

        rvHistoryList.adapter = adapter
        rvHistoryList.layoutManager = LinearLayoutManager(requireContext())

        // Button listeners
        btnClearHistory.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            showClearAllConfirmationDialog {
                refreshHistoryInOverlay(rvHistoryList, emptyStateLayout)
            }
        }

        btnCloseHistory.setOnClickListener {
            ButtonUtil.vibratePhone(requireContext())
            hideHistoryFromTop()
        }

        // Click outside to close
        historyView.setOnClickListener {
            hideHistoryFromTop()
        }

        // Prevent clicks from passing through
        rvHistoryList.setOnClickListener { /* Consume click */ }
        historyView.findViewById<LinearLayout>(R.id.emptyStateLayout)?.setOnClickListener { /* Consume */ }

        // Add to parent and animate
        historyView.translationY = -parentView.height.toFloat()
        parentView.addView(historyView)
        historyOverlayView = historyView
        isHistoryVisible = true

        // Slide down animation
        historyView.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        AnalyticsManager.log("history_overlay_opened")
    }

    // New method: Hide history with top-up animation
    private fun hideHistoryFromTop() {
        if (!isHistoryVisible || historyOverlayView == null) return

        val view = historyOverlayView!!
        val parentView = view.parent as? ViewGroup

        view.animate()
            .translationY(-view.height.toFloat())
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                parentView?.removeView(view)
                historyOverlayView = null
                isHistoryVisible = false
            }
            .start()

        AnalyticsManager.log("history_overlay_closed")
    }

    // New method: Animate result population with bounce effect
    private fun animateResultPopulation(result: String) {
        ButtonUtil.vibratePhone(requireContext())

        val startText = binding.tvPrimaryBC.text.toString()
        binding.tvPrimaryBC.text = result

        // Scale animation for visual feedback
        binding.tvPrimaryBC.apply {
            scaleX = 0.7f
            scaleY = 0.7f
            alpha = 0.5f

            animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .alpha(1f)
                .setDuration(200)
                .withEndAction {
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }

        validateAndUpdateUI()
        AnalyticsManager.log("history_result_populated", "result" to result)
    }

    // Replace the old showDeleteConfirmation method with this:
    private fun showDeleteConfirmationDialog(position: Int, onDeleted: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Calculation")
            .setMessage("Are you sure you want to delete this calculation?")
            .setPositiveButton("Delete") { _, _ ->
                HistoryManager.deleteHistoryItem(requireContext(), position)
                onDeleted()
                Toast.makeText(requireContext(), "Calculation deleted", Toast.LENGTH_SHORT).show()
                AnalyticsManager.log("history_item_deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // And update the clear all method:
    private fun showClearAllConfirmationDialog(onCleared: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear All History")
            .setMessage("Are you sure you want to delete all calculation history?")
            .setPositiveButton("Clear All") { _, _ ->
                HistoryManager.clearHistory(requireContext())
                onCleared()
                Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show()
                AnalyticsManager.log("history_cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshHistoryInOverlay(recyclerView: RecyclerView, emptyState: LinearLayout) {
        val historyList = HistoryManager.getHistory(requireContext())

        if (historyList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            (recyclerView.adapter as? HistoryBottomSheetAdapter)?.updateHistory(historyList)
        }
    }

    override fun onResume() {
        super.onResume()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (isHistoryVisible) {
                hideHistoryFromTop()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isHistoryVisible) {
            (historyOverlayView?.parent as? ViewGroup)?.removeView(historyOverlayView)
            historyOverlayView = null
            isHistoryVisible = false
        }
    }

    private fun refreshHistoryList(recyclerView: RecyclerView, emptyState: LinearLayout) {
        val historyList = HistoryManager.getHistory(requireContext())

        if (historyList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            (recyclerView.adapter as? HistoryBottomSheetAdapter)?.updateHistory(historyList)
        }
    }


    private fun addNewCalculationHistory(expression: String, result: String) {
        val newHistoryItem = CalculationHistory(expression, result)
        historyAdapter.addHistoryItem(newHistoryItem)
        recyclerView.scrollToPosition(historyAdapter.itemCount - 1)
    }

    private fun setupBasicButtons() {
        binding.apply {
            // Number buttons
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

            // Bracket buttons
            addNumberValueToText(requireContext(), btBracketOpenBC, tvPrimaryBC, 0)
            addNumberValueToText(requireContext(), btBracketCloseBC, tvPrimaryBC, 0)

            // Operator buttons
            addOperatorValueToText(requireContext(), btAdditionBC, tvPrimaryBC, "+", 0)
            addOperatorValueToText(requireContext(), btSubtractionBC, tvPrimaryBC, "-", 0)
            addOperatorValueToText(requireContext(), btMultiplicationBC, tvPrimaryBC, "*", 0)
            addOperatorValueToText(requireContext(), btDivisionBC, tvPrimaryBC, "/", 0)

            // Enhanced decimal point logic
            btDotBC.setOnClickListener {
                vibratePhone(requireContext())
                val currentText = tvPrimaryBC.text.toString()
                val lastNumber = getLastNumber(currentText)
                if (!lastNumber.contains(".")) {
                    addToExpressionHistory(currentText)
                    tvPrimaryBC.text = currentText + "."
                    validateAndUpdateUI()
                }
            }

            // Clear button
            btACBC.setOnClickListener {
                vibratePhone(requireContext())
                addToExpressionHistory(tvPrimaryBC.text.toString())
                tvPrimaryBC.text = ""
                tvSecondaryBC.text = ""
                addedBC = false
                clearError()
                resetCalculatorState()

                AnalyticsManager.log("calculator_cleared")
            }

            // Delete button
            btDeleteBC.setOnClickListener {
                vibratePhone(requireContext())
                val currentText = tvPrimaryBC.text.toString()
                if (currentText.isNotEmpty()) {
                    addToExpressionHistory(currentText)
                    val newText = currentText.subSequence(0, currentText.length - 1).toString()
                    tvPrimaryBC.text = newText
                    validateAndUpdateUI()

                    if (containsOperator(newText)) {
                        addedBC = false
                    }
                }
            }

            // Enhanced equals button
            btEqualBC.setOnClickListener {
                vibratePhone(requireContext())
                handleEqualsPress()
            }
        }
    }

    private fun setupScientificButtons() {
        binding.apply {
            btSecond.setOnClickListener {
                vibratePhone(requireContext())
                toggleSecondMode()
            }

            btDeg.setOnClickListener {
                vibratePhone(requireContext())
                toggleAngleMode()
            }

            btSin.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "asin" else "sin"
                onScientificFunctionClicked(function)
            }

            btCos.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "acos" else "cos"
                onScientificFunctionClicked(function)
            }

            btTan.setOnClickListener {
                vibratePhone(requireContext())
                val function = if (isSecondMode) "atan" else "atan"
                onScientificFunctionClicked(function)
            }

            btRootX.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("sqrt")
            }

            btPowerXY.setOnClickListener {
                vibratePhone(requireContext())
                handlePowerOperation()
            }

            btLg.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("lg")
            }

            btLn.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("ln")
            }

            btFactorial.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("factorial")
            }

            btInverse.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("inverse")
            }

            btPi.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("pi")
            }
        }
    }

    private fun setupAdvancedButtons() {
        binding.apply {
            btSinh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("sinh")
            }

            btCosh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("cosh")
            }

            btTanh.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("tanh")
            }

            btLog2.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("log2")
            }

            btE.setOnClickListener {
                vibratePhone(requireContext())
                onScientificFunctionClicked("e")
            }

            btScientificNotation.setOnClickListener {
                vibratePhone(requireContext())
                enableScientificNotation()
            }
        }
    }

    private fun setupMemoryButtons() {
        binding.apply {
            btMemoryAdd.setOnClickListener {
                vibratePhone(requireContext())
                val current = parseNumber(tvPrimaryBC.text.toString())
                if (current != null) {
                    memoryValue += current
                    showMemoryIndicator(memoryValue != 0.0)
                    saveMemoryState()

                    AnalyticsManager.log("memory_operation", "operation" to "add")
                }
            }

            btMemorySubtract.setOnClickListener {
                vibratePhone(requireContext())
                val current = parseNumber(tvPrimaryBC.text.toString())
                if (current != null) {
                    memoryValue -= current
                    showMemoryIndicator(memoryValue != 0.0)
                    saveMemoryState()

                    AnalyticsManager.log("memory_operation", "operation" to "subtract")
                }
            }

            btMemoryRecall.setOnClickListener {
                vibratePhone(requireContext())
                addToExpressionHistory(tvPrimaryBC.text.toString())
                tvPrimaryBC.text = smartFormatResult(memoryValue)
                validateAndUpdateUI()

                AnalyticsManager.log("memory_operation", "operation" to "recall")
            }

            btMemoryClear.setOnClickListener {
                vibratePhone(requireContext())
                memoryValue = 0.0
                showMemoryIndicator(false)
                saveMemoryState()

                AnalyticsManager.log("memory_operation", "operation" to "clear")
            }
        }
    }

    private fun setupControlButtons() {
        binding.apply {
            btUndo.setOnClickListener {
                vibratePhone(requireContext())
                val undoText = undo()
                if (undoText != null) {
                    tvPrimaryBC.text = undoText
                    validateAndUpdateUI()
                }
            }

            btRedo.setOnClickListener {
                vibratePhone(requireContext())
                val redoText = redo()
                if (redoText != null) {
                    tvPrimaryBC.text = redoText
                    validateAndUpdateUI()
                }
            }
        }
    }

    private fun handleEqualsPress() {
        if (isPowerMode && baseValue != null) {
            handlePowerCalculation()
        } else {
            handleRegularCalculation()
        }
    }

    private fun handlePowerCalculation() {
        val exponentInput = binding.tvPrimaryBC.text.toString().split("^").lastOrNull()?.toDoubleOrNull()

        if (exponentInput != null && baseValue != null) {
            val base = baseValue!!
            val result = base.pow(exponentInput)
            val historyExpression = "${base}^$exponentInput"

            binding.tvPrimaryBC.text = smartFormatResult(result)
            binding.tvSecondaryBC.text = "$historyExpression = ${smartFormatResult(result)}"

            // Reset power mode
            isPowerMode = false
            baseValue = null

            addNewCalculationHistory(historyExpression, smartFormatResult(result))
            AnalyticsManager.logCalculationPerformed("Basic Calculator", "power")
        } else {
            showError("Invalid power operation", ErrorType.SYNTAX)
        }
    }

    private fun handleRegularCalculation() {
        try {
            val input = binding.tvPrimaryBC.text.toString()
            if (input.isNotEmpty()) {
                val result = safeEvaluate(input)

                when (result) {
                    is CalculationResult.Success -> {
                        val formattedResult = smartFormatResult(result.value)
                        binding.tvPrimaryBC.text = formattedResult
                        binding.tvSecondaryBC.text = "$input = $formattedResult"
                        addedBC = false
                        clearError()

                        // Save to persistent history
                        HistoryManager.saveCalculation(requireContext(), input, formattedResult)

                        addNewCalculationHistory(input, formattedResult)
                        AnalyticsManager.logCalculationPerformed("Basic Calculator", "calculate")
                    }
                    is CalculationResult.Error -> {
                        showError(result.message, ErrorType.CALCULATION)
                    }
                }
            }
        } catch (e: Exception) {
            showError("Calculation error", ErrorType.CALCULATION)
        }
    }

    private fun onScientificFunctionClicked(function: String) {
        val currentInput = parseNumber(binding.tvPrimaryBC.text.toString())

        if (currentInput == null && function !in listOf("pi", "e")) {
            showError("Invalid input for function", ErrorType.SYNTAX)
            return
        }

        val result: Double = try {
            calculateScientificFunction(function, currentInput)
        } catch (e: Exception) {
            showError("Math error: ${e.message}", ErrorType.DOMAIN)
            return
        }

        if (result.isInfinite() || result.isNaN()) {
            showError("Invalid result", ErrorType.OVERFLOW)
            return
        }

        addToExpressionHistory(binding.tvPrimaryBC.text.toString())
        binding.tvPrimaryBC.text = smartFormatResult(result)

        val inputStr = currentInput?.toString() ?: ""
        val functionStr = getFunctionDisplayString(function, inputStr)
        binding.tvSecondaryBC.text = "$functionStr = ${smartFormatResult(result)}"

        AnalyticsManager.logCalculationPerformed("Basic Calculator", "scientific_$function")
    }

    private fun calculateScientificFunction(function: String, input: Double?): Double {
        return when (function) {
            "sin" -> if (isInDegreesMode) sin(Math.toRadians(input!!)) else sin(input!!)
            "cos" -> if (isInDegreesMode) cos(Math.toRadians(input!!)) else cos(input!!)
            "tan" -> if (isInDegreesMode) tan(Math.toRadians(input!!)) else tan(input!!)
            "asin" -> {
                validateInverseTrigInput(input!!)
                Math.toDegrees(asin(input))
            }
            "acos" -> {
                validateInverseTrigInput(input!!)
                Math.toDegrees(acos(input))
            }
            "atan" -> Math.toDegrees(atan(input!!))
            "sinh" -> sinh(input!!)
            "cosh" -> cosh(input!!)
            "tanh" -> tanh(input!!)
            "sqrt" -> {
                if (input!! < 0) throw ArithmeticException("Square root of negative number")
                sqrt(input)
            }
            "lg" -> {
                if (input!! <= 0) throw ArithmeticException("Logarithm of non-positive number")
                log10(input)
            }
            "ln" -> {
                if (input!! <= 0) throw ArithmeticException("Natural log of non-positive number")
                ln(input)
            }
            "log2" -> {
                if (input!! <= 0) throw ArithmeticException("Log base 2 of non-positive number")
                log2(input)
            }
            "factorial" -> calculateFactorial(input!!.toInt())
            "inverse" -> {
                if (input!! == 0.0) throw ArithmeticException("Division by zero")
                1 / input
            }
            "pi" -> if (input != null) input * PI else PI
            "e" -> if (input != null) input * E else E
            else -> input ?: 0.0
        }
    }

    private fun validateInverseTrigInput(input: Double) {
        if (input < -1 || input > 1) {
            throw ArithmeticException("Input out of domain for inverse trigonometric function")
        }
    }

    private fun calculateFactorial(n: Int): Double {
        if (n < 0) throw ArithmeticException("Factorial not defined for negative numbers")
        if (n > 170) throw ArithmeticException("Number too large for factorial")

        var result = 1.0
        for (i in 2..n) {
            result *= i
        }
        return result
    }

    private fun getFunctionDisplayString(function: String, input: String): String {
        return when (function) {
            "sin", "cos", "tan", "sinh", "cosh", "tanh", "asin", "acos", "atan" -> "$function($input)"
            "sqrt" -> "√($input)"
            "lg" -> "log($input)"
            "ln" -> "ln($input)"
            "log2" -> "log₂($input)"
            "factorial" -> "$input!"
            "inverse" -> "1/$input"
            "pi" -> if (input.isEmpty()) "π" else "$input×π"
            "e" -> if (input.isEmpty()) "e" else "$input×e"
            else -> function
        }
    }

    private fun handlePowerOperation() {
        val currentInput = parseNumber(binding.tvPrimaryBC.text.toString())

        if (currentInput != null) {
            baseValue = currentInput
            binding.tvPrimaryBC.text = "$currentInput^"
            isPowerMode = true
        } else {
            showError("Invalid input for power operation", ErrorType.SYNTAX)
        }
    }

    private fun toggleSecondMode() {
        isSecondMode = !isSecondMode

        binding.apply {
            if (isSecondMode) {
                btSin.text = "sin⁻¹"
                btCos.text = "cos⁻¹"
                btTan.text = "tan⁻¹"
                btDeg.isEnabled = false
                btDeg.alpha = 0.5f
            } else {
                btSin.text = "sin"
                btCos.text = "cos"
                btTan.text = "tan"
                btDeg.isEnabled = true
                btDeg.alpha = 1.0f
            }
        }

        AnalyticsManager.log("angle_mode_toggled", "mode" to if (isSecondMode) "inverse" else "normal")
    }

    private fun toggleAngleMode() {
        isInDegreesMode = !isInDegreesMode

        binding.btDeg.text = if (isInDegreesMode) "deg" else "rad"

        // In radian mode, disable second mode
        if (!isInDegreesMode) {
            disableSecondButton()
        } else {
            enableSecondButton()
        }

        AnalyticsManager.log("angle_unit_changed", "unit" to if (isInDegreesMode) "degrees" else "radians")
    }

    private fun enableScientificNotation() {
        val current = binding.tvPrimaryBC.text.toString()
        if (!current.contains("E") && current.isNotEmpty() && parseNumber(current) != null) {
            addToExpressionHistory(current)
            binding.tvPrimaryBC.text = "${current}E"
        }
    }

    // Validation methods
    private fun validateExpressionRealTime(expression: String): ValidationResult {
        return when {
            expression.isEmpty() -> ValidationResult.EMPTY
            hasConsecutiveOperators(expression) -> ValidationResult.CONSECUTIVE_OPERATORS
            hasUnbalancedParentheses(expression) -> ValidationResult.UNBALANCED_PARENTHESES
            endsWithOperator(expression) -> ValidationResult.INCOMPLETE
            hasInvalidCharacters(expression) -> ValidationResult.INVALID_CHARS
            else -> ValidationResult.VALID
        }
    }

    private fun hasConsecutiveOperators(expr: String): Boolean {
        return Regex("[+\\-*/]{2,}").containsMatchIn(expr)
    }

    private fun hasUnbalancedParentheses(expr: String): Boolean {
        var count = 0
        for (char in expr) {
            when (char) {
                '(' -> count++
                ')' -> if (--count < 0) return true
            }
        }
        return count != 0
    }

    private fun endsWithOperator(expr: String): Boolean {
        return expr.isNotEmpty() && expr.last() in listOf('+', '-', '*', '/', '^')
    }

    private fun hasInvalidCharacters(expr: String): Boolean {
        val validChars = "0123456789+-*/().^E°π"
        return expr.any { it !in validChars }
    }

    private fun containsOperator(text: String): Boolean {
        return text.contains("+") || text.contains("-") || text.contains("*") || text.contains("/")
    }

    private fun getLastNumber(expression: String): String {
        return expression.split(Regex("[+\\-*/()]")).lastOrNull() ?: ""
    }

    private fun parseNumber(text: String): Double? {
        return try {
            when {
                text.isEmpty() -> null
                text.contains("π") -> text.replace("π", PI.toString()).toDoubleOrNull()
                text.contains("e") && !text.contains("E") -> text.replace("e", E.toString()).toDoubleOrNull()
                text.endsWith("°") -> text.dropLast(1).toDoubleOrNull()
                else -> text.toDoubleOrNull()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun safeEvaluate(expression: String): CalculationResult {
        return try {
            val sanitized = sanitizeExpression(expression)
            val validation = validateExpressionRealTime(sanitized)

            if (validation != ValidationResult.VALID) {
                return CalculationResult.Error(validation.message)
            }

            val result = CalculationUtil.evaluate(sanitized)

            when {
                result.isInfinite() -> CalculationResult.Error("Result too large")
                result.isNaN() -> CalculationResult.Error("Invalid operation")
                else -> CalculationResult.Success(result.toDouble())
            }
        } catch (e: ArithmeticException) {
            CalculationResult.Error("Math error: ${e.message}")
        } catch (e: Exception) {
            CalculationResult.Error("Calculation error")
        }
    }

    private fun sanitizeExpression(expression: String): String {
        return expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", PI.toString())
            .replace(" ", "")
            .replace("°", "")
    }

    private fun smartFormatResult(result: Double): String {
        return when {
            result.isInfinite() -> if (result > 0) "∞" else "-∞"
            result.isNaN() -> "Error"
            result == 0.0 -> "0"
            abs(result) < 1e-10 -> "0"
            abs(result) >= 1e15 -> String.format("%.6E", result)
            result == result.toInt().toDouble() -> result.toInt().toString()
            else -> {
                val formatted = String.format("%.12f", result).trimEnd('0').trimEnd('.')
                if (formatted.length > 15) String.format("%.6E", result) else formatted
            }
        }
    }

    // Memory management
    private fun showMemoryIndicator(hasMemory: Boolean) {
        binding.memoryIndicator.visibility = if (hasMemory) View.VISIBLE else View.GONE
    }

    private fun saveMemoryState() {
        PrefUtil.setMemoryValue(requireContext(), memoryValue)
    }

    private fun restoreMemoryState() {
        memoryValue = PrefUtil.getMemoryValue(requireContext())
        showMemoryIndicator(memoryValue != 0.0)
    }

    // History management
    private fun addToExpressionHistory(expression: String) {
        if (expression.isNotEmpty() && (expressionHistory.isEmpty() || expressionHistory.last() != expression)) {
            if (historyIndex < expressionHistory.size - 1) {
                expressionHistory.subList(historyIndex + 1, expressionHistory.size).clear()
            }

            expressionHistory.add(expression)
            historyIndex = expressionHistory.size - 1

            if (expressionHistory.size > 50) {
                expressionHistory.removeAt(0)
                historyIndex--
            }
        }
    }

    private fun undo(): String? {
        return if (historyIndex > 0) {
            historyIndex--
            expressionHistory[historyIndex]
        } else null
    }

    private fun redo(): String? {
        return if (historyIndex < expressionHistory.size - 1) {
            historyIndex++
            expressionHistory[historyIndex]
        } else null
    }

    // Error handling
    private fun showError(message: String, type: ErrorType = ErrorType.CALCULATION) {
        binding.tvPrimaryBC.apply {
            text = when (type) {
                ErrorType.CALCULATION -> "Error"
                ErrorType.SYNTAX -> "Syntax Error"
                ErrorType.OVERFLOW -> "Overflow"
                ErrorType.DOMAIN -> "Math Error"
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.error_color))
        }

        binding.tvErrorBC.apply {
            text = message
            visibility = View.VISIBLE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            clearError()
        }, 3000)
    }

    private fun clearError() {
        binding.tvErrorBC.visibility = View.GONE
        // binding.tvPrimaryBC.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
    }

    // Real-time validation
    private fun validateAndUpdateUI() {
        inputRunnable?.let { inputHandler.removeCallbacks(it) }

        inputRunnable = Runnable {
            val expression = binding.tvPrimaryBC.text.toString()
            val validation = validateExpressionRealTime(expression)
            updateUIBasedOnValidation(validation)
        }

        inputHandler.postDelayed(inputRunnable!!, 300)
    }

    private fun updateUIBasedOnValidation(validation: ValidationResult) {
        if (validation != ValidationResult.VALID && validation != ValidationResult.EMPTY) {
            binding.tvErrorBC.apply {
                text = validation.message
                visibility = View.VISIBLE
            }
        } else {
            binding.tvErrorBC.visibility = View.GONE
        }
    }

    // Utility functions
    private fun disableSecondButton() {
        binding.btSecond.isEnabled = false
        binding.btSecond.alpha = 0.5f
    }

    private fun enableSecondButton() {
        binding.btSecond.isEnabled = true
        binding.btSecond.alpha = 1.0f
    }

    private fun resetCalculatorState() {
        isPowerMode = false
        baseValue = null
        isSecondMode = false
        // Keep memory and angle mode states
    }

    // Animation logic
    private fun toggleBarLogic() {
        binding.toggleBar.setOnClickListener {
            if (isPanelVisible) {
                slideDown(binding.scientificButtonsPanel)
                slideDown(binding.advancedScientificPanel)
                slideDown(binding.memoryControlPanel)
                binding.toggleArrow.rotation = 0f
            } else {
                slideUp(binding.scientificButtonsPanel)
                slideUp(binding.advancedScientificPanel)
                slideUp(binding.memoryControlPanel)
                binding.toggleArrow.rotation = 180f

                AnalyticsManager.log("scientific_panel_opened")
            }
            isPanelVisible = !isPanelVisible
        }
    }

    private fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    private fun slideDown(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat())
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }
            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    // Lifecycle methods
    override fun onStart() {
        super.onStart()
        binding.apply {
            val primaryText = PrefUtil.getPrimaryTextBC(requireContext()) ?: ""
            val secondaryText = PrefUtil.getSecondaryTextBC(requireContext()) ?: ""

            // Validate before restoring
            tvPrimaryBC.text = if (primaryText.isNotEmpty() && validateExpressionRealTime(primaryText) == ValidationResult.VALID) {
                primaryText
            } else {
                ""
            }
            tvSecondaryBC.text = secondaryText
        }
        restoreMemoryState()
    }

    override fun onStop() {
        super.onStop()
        binding.apply {
            PrefUtil.setPrimaryTextBC(requireContext(), tvPrimaryBC.text.toString())
            PrefUtil.setSecondaryTextBC(requireContext(), tvSecondaryBC.text.toString())
        }
        saveMemoryState()
    }

    override fun onDestroy() {
        super.onDestroy()
        inputRunnable?.let { inputHandler.removeCallbacks(it) }
    }
}