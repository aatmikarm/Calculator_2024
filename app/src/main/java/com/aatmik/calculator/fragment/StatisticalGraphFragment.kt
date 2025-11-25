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
import com.aatmik.calculator.databinding.FragmentStatisticalGraphBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class StatisticalGraphFragment : Fragment() {

    private lateinit var binding: FragmentStatisticalGraphBinding
    private val decimalFormat = DecimalFormat("#,##0.##")

    private val examples = mapOf(
        "Select Example..." to ExampleData("", "", ""),
        "Student Test Scores" to ExampleData(
            "65, 72, 78, 85, 88, 90, 92, 75, 68, 95, 82, 76, 84, 91, 73",
            "Test Scores Distribution",
            "Histogram of math test scores"
        ),
        "Monthly Sales" to ExampleData(
            "12000, 15000, 14500, 18000, 22000, 25000, 23000, 21000, 19000, 24000, 26000, 28000",
            "Monthly Sales Trend",
            "Sales data for 12 months"
        ),
        "Product Categories" to ExampleData(
            "Electronics: 35, Clothing: 28, Food: 22, Books: 15",
            "Product Distribution",
            "Sales by category"
        ),
        "Temperature Data" to ExampleData(
            "18, 20, 22, 25, 28, 32, 35, 33, 30, 26, 22, 19",
            "Annual Temperature",
            "Average monthly temperatures"
        ),
        "Survey Results" to ExampleData(
            "Yes: 65, No: 35",
            "Survey Response",
            "Do you support the proposal?"
        ),
        "Age Distribution" to ExampleData(
            "25, 28, 30, 32, 35, 38, 40, 42, 45, 48, 50, 52, 55, 28, 30",
            "Age Distribution",
            "Employee age data"
        )
    )

    private val graphTypes = arrayOf(
        "Bar Chart",
        "Histogram",
        "Pie Chart",
        "Line Graph"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStatisticalGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // Graph type spinner
        val graphTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, graphTypes)
        graphTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGraphType.adapter = graphTypeAdapter

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
    }

    private fun loadExample(exampleName: String) {
        examples[exampleName]?.let { data ->
            binding.etData.setText(data.dataValues)
            binding.etTitle.setText(data.title)
            binding.etDescription.setText(data.description)
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnGenerate.setOnClickListener {
                generateGraph()
            }

            btnClear.setOnClickListener {
                clearAll()
            }

            btnShare.setOnClickListener {
                if (graphCanvas.visibility == View.VISIBLE) {
                    shareGraph()
                } else {
                    Toast.makeText(context, "Please generate a graph first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateGraph() {
        val dataInput = binding.etData.text.toString().trim()
        val title = binding.etTitle.text.toString().trim()
        val graphType = binding.spinnerGraphType.selectedItemPosition

        if (dataInput.isEmpty()) {
            Toast.makeText(context, "Please enter data", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val parsedData = parseData(dataInput)

            if (parsedData.isEmpty()) {
                Toast.makeText(context, "Invalid data format", Toast.LENGTH_SHORT).show()
                return
            }

            when (graphType) {
                0 -> drawBarChart(parsedData, title)
                1 -> drawHistogram(parsedData, title)
                2 -> drawPieChart(parsedData, title)
                3 -> drawLineGraph(parsedData, title)
            }

            calculateStatistics(parsedData)
            binding.graphCanvas.visibility = View.VISIBLE
            binding.statisticsSection.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseData(input: String): Map<String, Double> {
        val data = mutableMapOf<String, Double>()

        // Check if labeled data (e.g., "Category: Value")
        if (input.contains(":")) {
            val items = input.split(",")
            items.forEachIndexed { index, item ->
                val parts = item.split(":")
                if (parts.size == 2) {
                    val label = parts[0].trim()
                    val value = parts[1].trim().toDoubleOrNull() ?: 0.0
                    data[label] = value
                } else {
                    data["Item ${index + 1}"] = item.trim().toDoubleOrNull() ?: 0.0
                }
            }
        } else {
            // Unlabeled numeric data
            val values = input.split(",")
            values.forEachIndexed { index, value ->
                val num = value.trim().toDoubleOrNull()
                if (num != null) {
                    data["${index + 1}"] = num
                }
            }
        }

        return data
    }

    private fun drawBarChart(data: Map<String, Double>, title: String) {
        val width = 900
        val height = 700
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

        // Calculate dimensions
        val chartTop = 100f
        val chartBottom = height - 120f
        val chartLeft = 100f
        val chartRight = width - 50f
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        val maxValue = data.values.maxOrNull() ?: 1.0
        val barWidth = chartWidth / data.size - 20f

        // Draw bars
        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#F44336"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#E91E63")
        )

        data.entries.forEachIndexed { index, entry ->
            val x = chartLeft + index * (barWidth + 20f) + 10f
            val barHeight = (entry.value / maxValue * chartHeight).toFloat()
            val y = chartBottom - barHeight

            paint.color = colors[index % colors.size]
            canvas.drawRect(x, y, x + barWidth, chartBottom, paint)

            // Value label on bar
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                decimalFormat.format(entry.value),
                x + barWidth / 2,
                y - 10f,
                paint
            )

            // X-axis label
            paint.textSize = 20f
            val label = if (entry.key.length > 8) entry.key.take(8) + "..." else entry.key
            canvas.drawText(label, x + barWidth / 2, chartBottom + 30f, paint)
        }

        // Y-axis
        paint.color = Color.BLACK
        paint.strokeWidth = 3f
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, paint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)

        // Y-axis labels
        paint.textSize = 20f
        paint.textAlign = Paint.Align.RIGHT
        for (i in 0..5) {
            val value = maxValue * i / 5
            val y = chartBottom - (chartHeight * i / 5)
            canvas.drawText(decimalFormat.format(value), chartLeft - 10f, y + 7f, paint)
            paint.strokeWidth = 1f
            paint.color = Color.LTGRAY
            canvas.drawLine(chartLeft, y, chartRight, y, paint)
            paint.color = Color.BLACK
            paint.strokeWidth = 3f
        }

        binding.graphCanvas.setImageBitmap(bitmap)
    }

    private fun drawHistogram(data: Map<String, Double>, title: String) {
        val values = data.values.toList()
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = max - min
        val binCount = minOf(10, values.size / 2 + 1)
        val binWidth = range / binCount

        // Create bins
        val bins = MutableList(binCount) { 0 }
        values.forEach { value ->
            val binIndex = minOf(((value - min) / binWidth).toInt(), binCount - 1)
            bins[binIndex]++
        }

        // Create labeled data for bars
        val binData = mutableMapOf<String, Double>()
        bins.forEachIndexed { index, count ->
            val binStart = min + index * binWidth
            val binEnd = binStart + binWidth
            val label = "${decimalFormat.format(binStart)}-${decimalFormat.format(binEnd)}"
            binData[label] = count.toDouble()
        }

        drawBarChart(binData, title)
    }

    private fun drawPieChart(data: Map<String, Double>, title: String) {
        val width = 900
        val height = 800
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

        val total = data.values.sum()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = 250f

        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#F44336"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#E91E63")
        )

        var startAngle = -90f

        // Draw pie slices
        data.entries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.value / total * 360).toFloat()

            paint.style = Paint.Style.FILL
            paint.color = colors[index % colors.size]

            val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)

            // Draw percentage label
            val labelAngle = startAngle + sweepAngle / 2
            val labelRadius = radius * 0.7f
            val labelX = centerX + labelRadius * cos(Math.toRadians(labelAngle.toDouble())).toFloat()
            val labelY = centerY + labelRadius * sin(Math.toRadians(labelAngle.toDouble())).toFloat()

            val percentage = (entry.value / total * 100)
            if (percentage > 3) { // Only show label if slice is large enough
                paint.color = Color.WHITE
                paint.textSize = 28f
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${decimalFormat.format(percentage)}%", labelX, labelY + 10, paint)
            }

            startAngle += sweepAngle
        }

        // Draw legend
        val legendX = 50f
        var legendY = height - 200f
        paint.textSize = 24f
        paint.textAlign = Paint.Align.LEFT

        data.entries.forEachIndexed { index, entry ->
            paint.style = Paint.Style.FILL
            paint.color = colors[index % colors.size]
            canvas.drawRect(legendX, legendY - 15, legendX + 30, legendY + 5, paint)

            paint.color = Color.BLACK
            val label = if (entry.key.length > 20) entry.key.take(20) + "..." else entry.key
            canvas.drawText("$label: ${decimalFormat.format(entry.value)}", legendX + 40, legendY, paint)

            legendY += 35f
        }

        binding.graphCanvas.setImageBitmap(bitmap)
    }

    private fun drawLineGraph(data: Map<String, Double>, title: String) {
        val width = 900
        val height = 700
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
        val chartBottom = height - 120f
        val chartLeft = 100f
        val chartRight = width - 50f
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        val values = data.values.toList()
        val maxValue = values.maxOrNull() ?: 1.0
        val minValue = values.minOrNull() ?: 0.0
        val valueRange = maxValue - minValue

        // Draw grid
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        for (i in 0..5) {
            val y = chartBottom - (chartHeight * i / 5)
            canvas.drawLine(chartLeft, y, chartRight, y, paint)
        }

        // Draw axes
        paint.color = Color.BLACK
        paint.strokeWidth = 3f
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, paint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)

        // Y-axis labels
        paint.textSize = 20f
        paint.textAlign = Paint.Align.RIGHT
        for (i in 0..5) {
            val value = minValue + valueRange * i / 5
            val y = chartBottom - (chartHeight * i / 5)
            canvas.drawText(decimalFormat.format(value), chartLeft - 10f, y + 7f, paint)
        }

        // Plot line
        val points = mutableListOf<Pair<Float, Float>>()
        values.forEachIndexed { index, value ->
            val x = chartLeft + (chartWidth * index / (values.size - 1))
            val y = chartBottom - ((value - minValue) / valueRange * chartHeight).toFloat()
            points.add(Pair(x, y))
        }

        // Draw line
        paint.color = Color.parseColor("#2196F3")
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        for (i in 0 until points.size - 1) {
            canvas.drawLine(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, paint)
        }

        // Draw points
        paint.style = Paint.Style.FILL
        points.forEach { point ->
            canvas.drawCircle(point.first, point.second, 8f, paint)
        }

        binding.graphCanvas.setImageBitmap(bitmap)
    }

    private fun calculateStatistics(data: Map<String, Double>) {
        val values = data.values.toList()

        val mean = values.average()
        val sorted = values.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }

        val mode = values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0.0
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val range = max - min

        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        val sum = values.sum()
        val count = values.size

        val stats = StringBuilder()
        stats.append("📊 STATISTICAL SUMMARY\n\n")
        stats.append("📈 Basic Statistics:\n")
        stats.append("• Count: $count data points\n")
        stats.append("• Sum: ${decimalFormat.format(sum)}\n")
        stats.append("• Mean (Average): ${decimalFormat.format(mean)}\n")
        stats.append("• Median: ${decimalFormat.format(median)}\n")
        stats.append("• Mode: ${decimalFormat.format(mode)}\n\n")

        stats.append("📏 Spread Measures:\n")
        stats.append("• Minimum: ${decimalFormat.format(min)}\n")
        stats.append("• Maximum: ${decimalFormat.format(max)}\n")
        stats.append("• Range: ${decimalFormat.format(range)}\n")
        stats.append("• Standard Deviation: ${decimalFormat.format(stdDev)}\n")
        stats.append("• Variance: ${decimalFormat.format(variance)}\n\n")

        stats.append("📋 Data Distribution:\n")
        if (data.size <= 10) {
            data.entries.forEachIndexed { index, entry ->
                val percentage = (entry.value / sum * 100)
                stats.append("• ${entry.key}: ${decimalFormat.format(entry.value)} (${decimalFormat.format(percentage)}%)\n")
            }
        } else {
            stats.append("• ${data.size} categories total\n")
            stats.append("• Top 3 values:\n")
            data.entries.sortedByDescending { it.value }.take(3).forEach { entry ->
                stats.append("  - ${entry.key}: ${decimalFormat.format(entry.value)}\n")
            }
        }

        binding.tvStatistics.text = stats.toString()
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
            val file = File(cachePath, "statistical_graph_$timestamp.png")

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
                putExtra(Intent.EXTRA_TEXT, "Graph: ${binding.etTitle.text}")
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
            etData.text?.clear()
            etTitle.text?.clear()
            etDescription.text?.clear()
            graphCanvas.visibility = View.GONE
            statisticsSection.visibility = View.GONE
        }
        Toast.makeText(context, "Cleared", Toast.LENGTH_SHORT).show()
    }

    data class ExampleData(
        val dataValues: String,
        val title: String,
        val description: String
    )
}