package com.aatmik.calculator.fragment

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCoordinateGeometryBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class CoordinateGeometryFragment : Fragment() {

    private lateinit var binding: FragmentCoordinateGeometryBinding
    private val decimalFormat = DecimalFormat("#,##0.##")

    private var xMin = -10.0
    private var xMax = 10.0
    private var yMin = -10.0
    private var yMax = 10.0

    private val examples = mapOf(
        "Select Example..." to ExampleData("", "", ""),
        "Line: y = 2x + 1" to ExampleData(
            "line: y = 2*x + 1",
            "Linear Equation",
            "Plot a straight line"
        ),
        "Circle: (x-2)² + (y-3)² = 16" to ExampleData(
            "circle: (2,3), r=4",
            "Circle",
            "Circle with center and radius"
        ),
        "Two Points Distance" to ExampleData(
            "points: (1,2), (4,6)",
            "Distance Calculation",
            "Find distance between two points"
        ),
        "Triangle Vertices" to ExampleData(
            "points: (0,0), (4,0), (2,3)",
            "Triangle",
            "Calculate area and perimeter"
        ),
        "Perpendicular Lines" to ExampleData(
            "line: y = 2*x + 1\nline: y = -0.5*x + 3",
            "Perpendicular Lines",
            "Lines with perpendicular slopes"
        ),
        "Parabola" to ExampleData(
            "parabola: y = x^2 - 4*x + 3",
            "Quadratic Curve",
            "Parabola opening upward"
        )
    )

    private val shapeTypes = arrayOf(
        "Point",
        "Line",
        "Circle",
        "Parabola"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCoordinateGeometryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // Shape type spinner
        val shapeTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, shapeTypes)
        shapeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerShapeType.adapter = shapeTypeAdapter

        // Examples spinner
        val exampleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, examples.keys.toList())
        exampleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerExamples.adapter = exampleAdapter

        binding.spinnerExamples.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedExample = examples.keys.toList()[position]
                loadExample(selectedExample)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerShapeType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInputHints(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateInputHints(shapeType: Int) {
        binding.apply {
            when (shapeType) {
                0 -> { // Point
                    etInput.hint = "e.g., (3, 4) or 3, 4"
                }
                1 -> { // Line
                    etInput.hint = "e.g., y = 2*x + 1 or (x1,y1), (x2,y2)"
                }
                2 -> { // Circle
                    etInput.hint = "e.g., center: (2,3), radius: 5"
                }
                3 -> { // Parabola
                    etInput.hint = "e.g., y = x^2 - 4*x + 3"
                }
            }
        }
    }

    private fun loadExample(exampleName: String) {
        examples[exampleName]?.let { data ->
            binding.etInput.setText(data.input)
            binding.etTitle.setText(data.title)
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnPlot.setOnClickListener {
                plotGeometry()
            }

            btnClear.setOnClickListener {
                clearAll()
            }

            btnShare.setOnClickListener {
                if (graphCanvas.visibility == View.VISIBLE) {
                    shareGraph()
                } else {
                    Toast.makeText(context, "Please plot something first", Toast.LENGTH_SHORT).show()
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

    private fun plotGeometry() {
        val input = binding.etInput.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()

        if (input.isEmpty()) {
            Toast.makeText(context, "Please enter coordinates or equation", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val shapes = parseInput(input)
            drawCoordinateSystem(shapes, title)
            analyzeShapes(shapes)

            binding.graphCanvas.visibility = View.VISIBLE
            binding.analysisSection.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseInput(input: String): List<Shape> {
        val shapes = mutableListOf<Shape>()
        val lines = input.split("\n")

        lines.forEach { line ->
            val trimmed = line.trim().lowercase()

            when {
                trimmed.startsWith("point") || trimmed.contains("(") && !trimmed.contains("=") -> {
                    val points = extractPoints(trimmed)
                    points.forEach { shapes.add(PointShape(it.first, it.second)) }
                }
                trimmed.startsWith("line") || trimmed.contains("y =") || trimmed.contains("y=") -> {
                    val lineShape = extractLine(trimmed)
                    if (lineShape != null) shapes.add(lineShape)
                }
                trimmed.startsWith("circle") -> {
                    val circle = extractCircle(trimmed)
                    if (circle != null) shapes.add(circle)
                }
                trimmed.startsWith("parabola") || (trimmed.contains("x^2") || trimmed.contains("x*x")) -> {
                    val parabola = extractParabola(trimmed)
                    if (parabola != null) shapes.add(parabola)
                }
            }
        }

        // If no specific type specified, try to parse as points
        if (shapes.isEmpty()) {
            val points = extractPoints(input)
            points.forEach { shapes.add(PointShape(it.first, it.second)) }
        }

        return shapes
    }

    private fun extractPoints(text: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val pattern = Regex("""\(?(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)\)?""")

        pattern.findAll(text).forEach { match ->
            val x = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val y = match.groupValues[2].toDoubleOrNull() ?: 0.0
            points.add(Pair(x, y))
        }

        return points
    }

    private fun extractLine(text: String): Shape? {
        // Check if two points format
        val points = extractPoints(text)
        if (points.size >= 2) {
            val (x1, y1) = points[0]
            val (x2, y2) = points[1]
            return LineShape(x1, y1, x2, y2)
        }

        // Check if equation format (y = mx + c)
        val eqPattern = Regex("""y\s*=\s*(.+)""")
        val match = eqPattern.find(text)
        if (match != null) {
            val equation = match.groupValues[1]
            return LineEquation(equation)
        }

        return null
    }

    private fun extractCircle(text: String): Shape? {
        val points = extractPoints(text)
        val radiusPattern = Regex("""r(?:adius)?\s*[:=]\s*(\d+\.?\d*)""")
        val radiusMatch = radiusPattern.find(text)

        if (points.isNotEmpty() && radiusMatch != null) {
            val (cx, cy) = points[0]
            val radius = radiusMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            return CircleShape(cx, cy, radius)
        }

        return null
    }

    private fun extractParabola(text: String): Shape? {
        val eqPattern = Regex("""y\s*=\s*(.+)""")
        val match = eqPattern.find(text)
        if (match != null) {
            val equation = match.groupValues[1]
            return ParabolaEquation(equation)
        }
        return null
    }

    private fun drawCoordinateSystem(shapes: List<Shape>, title: String) {
        val width = 900
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Title
        paint.color = Color.BLACK
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, width / 2f, 50f, paint)

        val chartTop = 100f
        val chartBottom = height - 50f
        val chartLeft = 50f
        val chartRight = width - 50f
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        // Draw grid
        drawGrid(canvas, chartLeft, chartTop, chartRight, chartBottom, paint)

        // Draw axes
        drawAxes(canvas, chartLeft, chartTop, chartRight, chartBottom, paint)

        // Plot shapes
        val scaleX = chartWidth / (xMax - xMin)
        val scaleY = chartHeight / (yMax - yMin)

        shapes.forEach { shape ->
            when (shape) {
                is PointShape -> drawPoint(canvas, shape, chartLeft, chartBottom, scaleX, scaleY, paint)
                is LineShape -> drawLine(canvas, shape, chartLeft, chartBottom, scaleX, scaleY, paint)
                is LineEquation -> drawLineEquation(canvas, shape, chartLeft, chartBottom, chartWidth, scaleX, scaleY, paint)
                is CircleShape -> drawCircle(canvas, shape, chartLeft, chartBottom, scaleX, scaleY, paint)
                is ParabolaEquation -> drawParabolaEquation(canvas, shape, chartLeft, chartBottom, chartWidth, scaleX, scaleY, paint)
            }
        }

        binding.graphCanvas.setImageBitmap(bitmap)
    }

    private fun drawGrid(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f

        val scaleX = (right - left) / (xMax - xMin)
        val scaleY = (bottom - top) / (yMax - yMin)

        // Vertical lines
        for (i in xMin.toInt()..xMax.toInt()) {
            val x = left + (i - xMin) * scaleX
            canvas.drawLine(x.toFloat(), top, x.toFloat(), bottom, paint)
        }

        // Horizontal lines
        for (i in yMin.toInt()..yMax.toInt()) {
            val y = bottom - (i - yMin) * scaleY
            canvas.drawLine(left, y.toFloat(), right, y.toFloat(), paint)
        }
    }

    private fun drawAxes(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        paint.color = Color.BLACK
        paint.strokeWidth = 3f

        val scaleX = (right - left) / (xMax - xMin)
        val scaleY = (bottom - top) / (yMax - yMin)

        // X-axis
        val yZero = bottom - (0 - yMin) * scaleY
        if (yZero in top..bottom) {
            canvas.drawLine(left, yZero.toFloat(), right, yZero.toFloat(), paint)
        }

        // Y-axis
        val xZero = left + (0 - xMin) * scaleX
        if (xZero in left..right) {
            canvas.drawLine(xZero.toFloat(), top, xZero.toFloat(), bottom, paint)
        }

        // Labels
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER

        // X-axis labels
        for (i in xMin.toInt()..xMax.toInt()) {
            if (i != 0) {
                val x = left + (i - xMin) * scaleX
                canvas.drawText(i.toString(), x.toFloat(), yZero.toFloat() + 25, paint)
            }
        }

        // Y-axis labels
        for (i in yMin.toInt()..yMax.toInt()) {
            if (i != 0) {
                val y = bottom - (i - yMin) * scaleY
                canvas.drawText(i.toString(), xZero.toFloat() - 25, y.toFloat() + 7, paint)
            }
        }

        // Origin
        canvas.drawText("0", xZero.toFloat() - 15, yZero.toFloat() + 20, paint)
    }

    private fun drawPoint(canvas: Canvas, point: PointShape, left: Float, bottom: Float,
                          scaleX: Double, scaleY: Double, paint: Paint) {
        val x = (left + (point.x - xMin) * scaleX).toFloat()
        val y = (bottom - (point.y - yMin) * scaleY).toFloat()

        paint.color = Color.parseColor("#F44336")
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 10f, paint)

        // Label
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("(${decimalFormat.format(point.x)}, ${decimalFormat.format(point.y)})",
            x + 15, y - 10, paint)
    }

    private fun drawLine(canvas: Canvas, line: LineShape, left: Float, bottom: Float,
                         scaleX: Double, scaleY: Double, paint: Paint) {
        val x1 = (left + (line.x1 - xMin) * scaleX).toFloat()
        val y1 = (bottom - (line.y1 - yMin) * scaleY).toFloat()
        val x2 = (left + (line.x2 - xMin) * scaleX).toFloat()
        val y2 = (bottom - (line.y2 - yMin) * scaleY).toFloat()

        paint.color = Color.parseColor("#2196F3")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x1, y1, x2, y2, paint)

        // Draw endpoints
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x1, y1, 8f, paint)
        canvas.drawCircle(x2, y2, 8f, paint)
    }

    private fun drawLineEquation(canvas: Canvas, line: LineEquation, left: Float, bottom: Float,
                                 width: Float, scaleX: Double, scaleY: Double, paint: Paint) {
        paint.color = Color.parseColor("#2196F3")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE

        var prevX: Float? = null
        var prevY: Float? = null

        for (px in 0..width.toInt()) {
            val x = xMin + px * (xMax - xMin) / width
            val y = evaluateExpression(line.equation, x)

            if (!y.isNaN() && !y.isInfinite()) {
                val screenX = (left + px).toFloat()
                val screenY = (bottom - (y - yMin) * scaleY).toFloat()

                if (prevX != null && prevY != null) {
                    canvas.drawLine(prevX, prevY, screenX, screenY, paint)
                }
                prevX = screenX
                prevY = screenY
            }
        }
    }

    private fun drawCircle(canvas: Canvas, circle: CircleShape, left: Float, bottom: Float,
                           scaleX: Double, scaleY: Double, paint: Paint) {
        val cx = (left + (circle.cx - xMin) * scaleX).toFloat()
        val cy = (bottom - (circle.cy - yMin) * scaleY).toFloat()
        val radius = (circle.radius * scaleX).toFloat()

        paint.color = Color.parseColor("#4CAF50")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, radius, paint)

        // Center point
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, 8f, paint)

        // Label
        paint.color = Color.BLACK
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("C(${decimalFormat.format(circle.cx)}, ${decimalFormat.format(circle.cy)})",
            cx, cy - radius - 10, paint)
    }

    private fun drawParabolaEquation(canvas: Canvas, parabola: ParabolaEquation, left: Float, bottom: Float,
                                     width: Float, scaleX: Double, scaleY: Double, paint: Paint) {
        paint.color = Color.parseColor("#FF9800")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE

        var prevX: Float? = null
        var prevY: Float? = null

        for (px in 0..width.toInt()) {
            val x = xMin + px * (xMax - xMin) / width
            val y = evaluateExpression(parabola.equation, x)

            if (!y.isNaN() && !y.isInfinite()) {
                val screenX = (left + px).toFloat()
                val screenY = (bottom - (y - yMin) * scaleY).toFloat()

                if (prevX != null && prevY != null && abs(screenY - prevY) < 100) {
                    canvas.drawLine(prevX, prevY, screenX, screenY, paint)
                }
                prevX = screenX
                prevY = screenY
            }
        }
    }

    private fun evaluateExpression(expr: String, x: Double): Double {
        return try {
            var result = expr.replace("x", x.toString())
            result = result.replace("^", "")

            // Handle x^2 or x*x
            if (expr.contains("x^2") || expr.contains("x*x")) {
                result = expr.replace("x^2", (x * x).toString())
                result = result.replace("x*x", (x * x).toString())
                result = result.replace("x", x.toString())
            }

            // Simple evaluation
            when {
                result.contains("+") -> {
                    val parts = result.split("+")
                    parts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }
                }
                result.contains("-") && result.indexOf("-") > 0 -> {
                    val parts = result.split("-")
                    parts[0].toDouble() - parts.drop(1).sumOf { it.toDoubleOrNull() ?: 0.0 }
                }
                result.contains("*") -> {
                    val parts = result.split("*")
                    parts.fold(1.0) { acc, s -> acc * (s.toDoubleOrNull() ?: 1.0) }
                }
                else -> result.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun analyzeShapes(shapes: List<Shape>) {
        val analysis = StringBuilder()

        analysis.append("📐 GEOMETRY ANALYSIS\n\n")

        shapes.forEachIndexed { index, shape ->
            analysis.append("Shape ${index + 1}: ")

            when (shape) {
                is PointShape -> {
                    analysis.append("Point\n")
                    analysis.append("• Coordinates: (${decimalFormat.format(shape.x)}, ${decimalFormat.format(shape.y)})\n")
                    analysis.append("• Quadrant: ${getQuadrant(shape.x, shape.y)}\n\n")
                }
                is LineShape -> {
                    analysis.append("Line Segment\n")
                    val slope = (shape.y2 - shape.y1) / (shape.x2 - shape.x1)
                    val length = sqrt((shape.x2 - shape.x1).pow(2) + (shape.y2 - shape.y1).pow(2))
                    val midX = (shape.x1 + shape.x2) / 2
                    val midY = (shape.y1 + shape.y2) / 2

                    analysis.append("• Point 1: (${decimalFormat.format(shape.x1)}, ${decimalFormat.format(shape.y1)})\n")
                    analysis.append("• Point 2: (${decimalFormat.format(shape.x2)}, ${decimalFormat.format(shape.y2)})\n")
                    analysis.append("• Slope: ${decimalFormat.format(slope)}\n")
                    analysis.append("• Length: ${decimalFormat.format(length)} units\n")
                    analysis.append("• Midpoint: (${decimalFormat.format(midX)}, ${decimalFormat.format(midY)})\n\n")
                }
                is LineEquation -> {
                    analysis.append("Line (Equation)\n")
                    analysis.append("• Equation: y = ${shape.equation}\n\n")
                }
                is CircleShape -> {
                    analysis.append("Circle\n")
                    val area = PI * shape.radius * shape.radius
                    val circumference = 2 * PI * shape.radius

                    analysis.append("• Center: (${decimalFormat.format(shape.cx)}, ${decimalFormat.format(shape.cy)})\n")
                    analysis.append("• Radius: ${decimalFormat.format(shape.radius)} units\n")
                    analysis.append("• Diameter: ${decimalFormat.format(2 * shape.radius)} units\n")
                    analysis.append("• Area: ${decimalFormat.format(area)} square units\n")
                    analysis.append("• Circumference: ${decimalFormat.format(circumference)} units\n\n")
                }
                is ParabolaEquation -> {
                    analysis.append("Parabola\n")
                    analysis.append("• Equation: y = ${shape.equation}\n\n")
                }
            }
        }

        // Additional calculations for multiple points
        val points = shapes.filterIsInstance<PointShape>()
        if (points.size >= 2) {
            analysis.append("📏 Additional Calculations:\n\n")

            if (points.size == 2) {
                val dist = sqrt((points[1].x - points[0].x).pow(2) + (points[1].y - points[0].y).pow(2))
                analysis.append("Distance between points: ${decimalFormat.format(dist)} units\n")
            }

            if (points.size == 3) {
                val a = sqrt((points[1].x - points[0].x).pow(2) + (points[1].y - points[0].y).pow(2))
                val b = sqrt((points[2].x - points[1].x).pow(2) + (points[2].y - points[1].y).pow(2))
                val c = sqrt((points[0].x - points[2].x).pow(2) + (points[0].y - points[2].y).pow(2))
                val s = (a + b + c) / 2
                val area = sqrt(s * (s - a) * (s - b) * (s - c))

                analysis.append("Triangle formed by 3 points:\n")
                analysis.append("• Side lengths: ${decimalFormat.format(a)}, ${decimalFormat.format(b)}, ${decimalFormat.format(c)}\n")
                analysis.append("• Perimeter: ${decimalFormat.format(a + b + c)} units\n")
                analysis.append("• Area: ${decimalFormat.format(area)} square units\n")
            }
        }

        binding.tvAnalysis.text = analysis.toString()
    }

    private fun getQuadrant(x: Double, y: Double): String {
        return when {
            x > 0 && y > 0 -> "I (positive x, positive y)"
            x < 0 && y > 0 -> "II (negative x, positive y)"
            x < 0 && y < 0 -> "III (negative x, negative y)"
            x > 0 && y < 0 -> "IV (positive x, negative y)"
            x == 0.0 && y != 0.0 -> "On Y-axis"
            y == 0.0 && x != 0.0 -> "On X-axis"
            else -> "Origin"
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

        val input = binding.etInput.text.toString()
        if (input.isNotEmpty()) {
            plotGeometry()
        }
    }

    private fun resetView() {
        xMin = -10.0
        xMax = 10.0
        yMin = -10.0
        yMax = 10.0

        val input = binding.etInput.text.toString()
        if (input.isNotEmpty()) {
            plotGeometry()
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
            val file = File(cachePath, "coordinate_geometry_$timestamp.png")

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
                putExtra(Intent.EXTRA_TEXT, "Geometry: ${binding.etTitle.text}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share via"))

        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAll() {
        binding.apply {
            spinnerExamples.setSelection(0)
            etInput.text?.clear()
            etTitle.text?.clear()
            graphCanvas.visibility = View.GONE
            analysisSection.visibility = View.GONE
        }
        resetView()
        Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
    }

    // Shape classes
    sealed class Shape
    data class PointShape(val x: Double, val y: Double) : Shape()
    data class LineShape(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Shape()
    data class LineEquation(val equation: String) : Shape()
    data class CircleShape(val cx: Double, val cy: Double, val radius: Double) : Shape()
    data class ParabolaEquation(val equation: String) : Shape()

    data class ExampleData(
        val input: String,
        val title: String,
        val description: String
    )
}