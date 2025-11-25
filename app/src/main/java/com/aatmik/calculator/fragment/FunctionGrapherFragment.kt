package com.aatmik.calculator.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentFunctionGrapherBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class FunctionGrapherFragment : Fragment() {

    private lateinit var binding: FragmentFunctionGrapherBinding
    private val decimalFormat = DecimalFormat("#,##0.##")

    private var xMin = -10.0
    private var xMax = 10.0
    private var yMin = -10.0
    private var yMax = 10.0

    private val colors = listOf(
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#9C27B0")  // Purple
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFunctionGrapherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupListeners()
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnPlot.setOnClickListener {
                plotGraph()
            }

            btnClear.setOnClickListener {
                clearAll()
            }

            btnShare.setOnClickListener {
                if (graphCanvas.visibility == View.VISIBLE) {
                    shareGraph()
                } else {
                    Toast.makeText(context, "Please plot a graph first", Toast.LENGTH_SHORT).show()
                }
            }

            btnZoomIn.setOnClickListener {
                zoom(0.8)
            }

            btnZoomOut.setOnClickListener {
                zoom(1.25)
            }

            btnReset.setOnClickListener {
                resetView()
            }
        }
    }

    private fun plotGraph() {
        val equation = binding.etEquation.text.toString().trim()

        if (equation.isEmpty()) {
            Toast.makeText(context, "Please enter an equation", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse range if provided
        val xMinText = binding.etXMin.text.toString()
        val xMaxText = binding.etXMax.text.toString()

        if (xMinText.isNotEmpty()) {
            xMin = xMinText.toDoubleOrNull() ?: -10.0
        }
        if (xMaxText.isNotEmpty()) {
            xMax = xMaxText.toDoubleOrNull() ?: 10.0
        }

        try {
            drawGraph(equation)
            analyzeFunction(equation)
            binding.graphCanvas.visibility = View.VISIBLE
            binding.analysisSection.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawGraph(equation: String) {
        val width = 800
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 3f
        }

        // Draw grid
        drawGrid(canvas, width, height, paint)

        // Draw axes
        drawAxes(canvas, width, height, paint)

        // Plot function
        plotFunction(canvas, width, height, equation, paint)

        binding.graphCanvas.setImageBitmap(bitmap)
    }

    private fun drawGrid(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f

        val scaleX = width / (xMax - xMin)
        val scaleY = height / (yMax - yMin)

        // Vertical lines
        for (i in xMin.toInt()..xMax.toInt()) {
            val x = ((i - xMin) * scaleX).toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }

        // Horizontal lines
        for (i in yMin.toInt()..yMax.toInt()) {
            val y = (height - (i - yMin) * scaleY).toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
    }

    private fun drawAxes(canvas: Canvas, width: Int, height: Int, paint: Paint) {
        paint.color = Color.BLACK
        paint.strokeWidth = 3f

        val scaleX = width / (xMax - xMin)
        val scaleY = height / (yMax - yMin)

        // X-axis
        val yZero = (height - (0 - yMin) * scaleY).toFloat()
        if (yZero in 0f..height.toFloat()) {
            canvas.drawLine(0f, yZero, width.toFloat(), yZero, paint)
        }

        // Y-axis
        val xZero = ((0 - xMin) * scaleX).toFloat()
        if (xZero in 0f..width.toFloat()) {
            canvas.drawLine(xZero, 0f, xZero, height.toFloat(), paint)
        }

        // Draw labels
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER

        // X-axis labels
        for (i in xMin.toInt()..xMax.toInt()) {
            if (i != 0) {
                val x = ((i - xMin) * scaleX).toFloat()
                canvas.drawText(i.toString(), x, yZero + 30, paint)
            }
        }

        // Y-axis labels
        for (i in yMin.toInt()..yMax.toInt()) {
            if (i != 0) {
                val y = (height - (i - yMin) * scaleY).toFloat()
                canvas.drawText(i.toString(), xZero - 30, y + 8, paint)
            }
        }
    }

    private fun plotFunction(canvas: Canvas, width: Int, height: Int, equation: String, paint: Paint) {
        paint.color = colors[0]
        paint.strokeWidth = 4f

        val scaleX = width / (xMax - xMin)
        val scaleY = height / (yMax - yMin)
        val step = (xMax - xMin) / width

        var prevX: Float? = null
        var prevY: Float? = null

        for (px in 0 until width) {
            val x = xMin + px * step
            val y = evaluateFunction(equation, x)

            if (!y.isNaN() && !y.isInfinite()) {
                val screenX = px.toFloat()
                val screenY = (height - (y - yMin) * scaleY).toFloat()

                if (screenY in 0f..height.toFloat()) {
                    if (prevX != null && prevY != null &&
                        abs(screenY - prevY) < height / 2) {
                        canvas.drawLine(prevX, prevY, screenX, screenY, paint)
                    }
                    prevX = screenX
                    prevY = screenY
                } else {
                    prevX = null
                    prevY = null
                }
            } else {
                prevX = null
                prevY = null
            }
        }
    }

    private fun evaluateFunction(equation: String, x: Double): Double {
        return try {
            var expr = equation.lowercase()
                .replace("y=", "")
                .replace("f(x)=", "")
                .replace("^", "")
                .trim()

            // Replace mathematical functions
            expr = expr.replace("sin", "sin_")
            expr = expr.replace("cos", "cos_")
            expr = expr.replace("tan", "tan_")
            expr = expr.replace("sqrt", "sqrt_")
            expr = expr.replace("abs", "abs_")
            expr = expr.replace("ln", "ln_")
            expr = expr.replace("log", "log_")
            expr = expr.replace("exp", "exp_")

            // Replace x with value
            expr = expr.replace("x", x.toString())

            // Evaluate
            var result = parseExpression(expr)

            // Apply functions
            if (expr.contains("sin_")) result = sin(result)
            if (expr.contains("cos_")) result = cos(result)
            if (expr.contains("tan_")) result = tan(result)
            if (expr.contains("sqrt_")) result = sqrt(result)
            if (expr.contains("abs_")) result = abs(result)
            if (expr.contains("ln_")) result = ln(result)
            if (expr.contains("log_")) result = log10(result)
            if (expr.contains("exp_")) result = exp(result)

            result
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun parseExpression(expr: String): Double {
        // Simple expression parser for basic arithmetic
        return try {
            // Remove function markers
            var clean = expr.replace("sin_", "")
                .replace("cos_", "")
                .replace("tan_", "")
                .replace("sqrt_", "")
                .replace("abs_", "")
                .replace("ln_", "")
                .replace("log_", "")
                .replace("exp_", "")
                .replace("(", "")
                .replace(")", "")

            when {
                clean.contains("+") -> {
                    val parts = clean.split("+")
                    parts.sumOf { it.toDoubleOrNull() ?: 0.0 }
                }
                clean.contains("-") && clean.indexOf("-") > 0 -> {
                    val parts = clean.split("-")
                    parts[0].toDouble() - parts[1].toDouble()
                }
                clean.contains("*") -> {
                    val parts = clean.split("*")
                    parts.fold(1.0) { acc, s -> acc * (s.toDoubleOrNull() ?: 1.0) }
                }
                clean.contains("/") -> {
                    val parts = clean.split("/")
                    parts[0].toDouble() / parts[1].toDouble()
                }
                else -> clean.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun analyzeFunction(equation: String) {
        val analysis = StringBuilder()

        analysis.append("📊 FUNCTION ANALYSIS\n\n")

        // Function info
        analysis.append("📐 Function:\n")
        analysis.append("• Equation: $equation\n")
        analysis.append("• Domain: [${decimalFormat.format(xMin)}, ${decimalFormat.format(xMax)}]\n")
        analysis.append("• Range: [${decimalFormat.format(yMin)}, ${decimalFormat.format(yMax)}]\n\n")

        // Find key points
        analysis.append("🎯 Key Points:\n")

        // Y-intercept (x=0)
        val yIntercept = evaluateFunction(equation, 0.0)
        if (!yIntercept.isNaN() && !yIntercept.isInfinite()) {
            analysis.append("• Y-intercept: (0, ${decimalFormat.format(yIntercept)})\n")
        }

        // Find approximate roots (x-intercepts)
        val roots = findRoots(equation)
        if (roots.isNotEmpty()) {
            analysis.append("• X-intercepts:\n")
            roots.forEach { root ->
                analysis.append("  - x ≈ ${decimalFormat.format(root)}\n")
            }
        }

        // Find extrema
        val extrema = findExtrema(equation)
        if (extrema.isNotEmpty()) {
            analysis.append("• Critical Points:\n")
            extrema.forEach { (x, y) ->
                analysis.append("  - (${decimalFormat.format(x)}, ${decimalFormat.format(y)})\n")
            }
        }

        analysis.append("\n")

        // Function type
        analysis.append("📋 Function Type:\n")
        val type = classifyFunction(equation)
        analysis.append("• $type\n\n")

        // Properties
        analysis.append("✨ Properties:\n")
        analysis.append("• Continuous: ${isContinuous(equation)}\n")
        analysis.append("• Differentiable: Yes (smooth curve)\n")

        binding.tvAnalysis.text = analysis.toString()
    }

    private fun findRoots(equation: String): List<Double> {
        val roots = mutableListOf<Double>()
        val step = (xMax - xMin) / 1000
        var prevY = evaluateFunction(equation, xMin)

        for (i in 1..1000) {
            val x = xMin + i * step
            val y = evaluateFunction(equation, x)

            if (!prevY.isNaN() && !y.isNaN() &&
                !prevY.isInfinite() && !y.isInfinite()) {
                if (prevY * y < 0) { // Sign change
                    roots.add(x - step / 2)
                }
            }
            prevY = y
        }

        return roots.take(5) // Limit to 5 roots
    }

    private fun findExtrema(equation: String): List<Pair<Double, Double>> {
        val extrema = mutableListOf<Pair<Double, Double>>()
        val step = (xMax - xMin) / 1000
        val h = step

        for (i in 1..999) {
            val x = xMin + i * step
            val y = evaluateFunction(equation, x)
            val yPrev = evaluateFunction(equation, x - h)
            val yNext = evaluateFunction(equation, x + h)

            if (!y.isNaN() && !yPrev.isNaN() && !yNext.isNaN() &&
                !y.isInfinite() && !yPrev.isInfinite() && !yNext.isInfinite()) {
                // Check for local maximum or minimum
                if ((y > yPrev && y > yNext) || (y < yPrev && y < yNext)) {
                    extrema.add(Pair(x, y))
                }
            }
        }

        return extrema.take(5) // Limit to 5 extrema
    }

    private fun classifyFunction(equation: String): String {
        val lower = equation.lowercase()
        return when {
            lower.contains("sin") -> "Trigonometric (Sine)"
            lower.contains("cos") -> "Trigonometric (Cosine)"
            lower.contains("tan") -> "Trigonometric (Tangent)"
            lower.contains("exp") || lower.contains("e^") -> "Exponential"
            lower.contains("ln") || lower.contains("log") -> "Logarithmic"
            lower.contains("sqrt") -> "Square Root / Radical"
            lower.contains("abs") -> "Absolute Value"
            lower.contains("x^3") || lower.contains("x*x*x") -> "Cubic Polynomial"
            lower.contains("x^2") || lower.contains("x*x") -> "Quadratic / Parabola"
            lower.contains("1/x") || lower.contains("x^-1") -> "Rational / Hyperbola"
            lower.contains("x") && !lower.contains("^") -> "Linear"
            else -> "General Function"
        }
    }

    private fun isContinuous(equation: String): String {
        val lower = equation.lowercase()
        return when {
            lower.contains("1/x") || lower.contains("tan") -> "No (has discontinuities)"
            lower.contains("sqrt") -> "Yes (on valid domain)"
            lower.contains("ln") || lower.contains("log") -> "Yes (for x > 0)"
            else -> "Yes"
        }
    }

    private fun zoom(factor: Double) {
        val xCenter = (xMax + xMin) / 2
        val yCenter = (yMax + yMin) / 2
        val xRange = (xMax - xMin) * factor
        val yRange = (yMax - yMin) * factor

        xMin = xCenter - xRange / 2
        xMax = xCenter + xRange / 2
        yMin = yCenter - yRange / 2
        yMax = yCenter + yRange / 2

        val equation = binding.etEquation.text.toString()
        if (equation.isNotEmpty()) {
            drawGraph(equation)
        }
    }

    private fun resetView() {
        xMin = -10.0
        xMax = 10.0
        yMin = -10.0
        yMax = 10.0

        val equation = binding.etEquation.text.toString()
        if (equation.isNotEmpty()) {
            drawGraph(equation)
        }
    }

    private fun shareGraph() {
        val options = arrayOf("Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Graph")
            .setItems(options) { dialog, _ ->
                captureAndShareGraph()
                dialog.dismiss()
            }
            .show()
    }

    private fun captureAndShareGraph() {
        try {
            val bitmap = (binding.graphCanvas.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap

            if (bitmap == null) {
                Toast.makeText(context, "No graph to share", Toast.LENGTH_SHORT).show()
                return
            }

            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "function_graph_$timestamp.png")

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "Function: ${binding.etEquation.text}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share via"))

        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAll() {
        binding.apply {
            etEquation.text?.clear()
            etXMin.text?.clear()
            etXMax.text?.clear()
            graphCanvas.visibility = View.GONE
            analysisSection.visibility = View.GONE
        }
        resetView()
        Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
    }
}