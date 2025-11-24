package com.aatmik.calculator.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.aatmik.calculator.databinding.FragmentPunnettSquareBinding
import com.google.android.material.chip.Chip

class PunnettSquareFragment : Fragment() {

    private lateinit var binding: FragmentPunnettSquareBinding
    private var currentCrossType = CrossType.MONOHYBRID
    private lateinit var punnettAdapter: PunnettSquareAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.apply {
            // Handle back button
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Setup cross type chips
            setupCrossTypeChips()

            // Setup input listeners
            etParent1.addTextChangedListener(parentTextWatcher)
            etParent2.addTextChangedListener(parentTextWatcher)

            // Calculate button
            btnCalculate.setOnClickListener {
                calculatePunnettSquare()
            }

            // Example button
            btnExample.setOnClickListener {
                loadExample()
            }

            // Clear button
            btnClear.setOnClickListener {
                clearInputs()
            }
        }
    }

    private fun setupCrossTypeChips() {
        binding.apply {
            chipMonohybrid.setOnClickListener {
                selectCrossType(CrossType.MONOHYBRID)
            }
            chipDihybrid.setOnClickListener {
                selectCrossType(CrossType.DIHYBRID)
            }
            chipTrihybrid.setOnClickListener {
                selectCrossType(CrossType.TRIHYBRID)
            }

            // Set initial selection
            selectCrossType(CrossType.MONOHYBRID)
        }
    }

    private fun selectCrossType(crossType: CrossType) {
        currentCrossType = crossType

        binding.apply {
            // Update chip selection states
            chipMonohybrid.isChecked = crossType == CrossType.MONOHYBRID
            chipDihybrid.isChecked = crossType == CrossType.DIHYBRID
            chipTrihybrid.isChecked = crossType == CrossType.TRIHYBRID

            // Update hint text
            when (crossType) {
                CrossType.MONOHYBRID -> {
                    parent1InputLayout.hint = "Parent 1 (e.g., Aa)"
                    parent2InputLayout.hint = "Parent 2 (e.g., Aa)"
                }
                CrossType.DIHYBRID -> {
                    parent1InputLayout.hint = "Parent 1 (e.g., AaBb)"
                    parent2InputLayout.hint = "Parent 2 (e.g., AaBb)"
                }
                CrossType.TRIHYBRID -> {
                    parent1InputLayout.hint = "Parent 1 (e.g., AaBbCc)"
                    parent2InputLayout.hint = "Parent 2 (e.g., AaBbCc)"
                }
            }

            // Clear previous results
            resultsCard.visibility = View.GONE
            gridCard.visibility = View.GONE
        }
    }

    private fun loadExample() {
        binding.apply {
            when (currentCrossType) {
                CrossType.MONOHYBRID -> {
                    etParent1.setText("Aa")
                    etParent2.setText("Aa")
                }
                CrossType.DIHYBRID -> {
                    etParent1.setText("AaBb")
                    etParent2.setText("AaBb")
                }
                CrossType.TRIHYBRID -> {
                    etParent1.setText("AaBbCc")
                    etParent2.setText("AaBbCc")
                }
            }
            calculatePunnettSquare()
        }
    }

    private fun clearInputs() {
        binding.apply {
            etParent1.text?.clear()
            etParent2.text?.clear()
            resultsCard.visibility = View.GONE
            gridCard.visibility = View.GONE
        }
    }

    private fun calculatePunnettSquare() {
        val parent1 = binding.etParent1.text.toString().trim()
        val parent2 = binding.etParent2.text.toString().trim()

        // Validate inputs
        if (parent1.isEmpty() || parent2.isEmpty()) {
            Toast.makeText(context, "Please enter both parent genotypes", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidGenotype(parent1, currentCrossType) || !isValidGenotype(parent2, currentCrossType)) {
            Toast.makeText(context, "Invalid genotype format for ${currentCrossType.name}", Toast.LENGTH_LONG).show()
            return
        }

        // Calculate
        try {
            val calculator = PunnettSquareCalculator()
            val result = calculator.calculate(parent1, parent2, currentCrossType)

            displayResults(result)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isValidGenotype(genotype: String, crossType: CrossType): Boolean {
        val expectedLength = when (crossType) {
            CrossType.MONOHYBRID -> 2
            CrossType.DIHYBRID -> 4
            CrossType.TRIHYBRID -> 6
        }

        if (genotype.length != expectedLength) return false

        // Check format: alternating uppercase and lowercase (Aa, AaBb, AaBbCc)
        for (i in genotype.indices step 2) {
            if (i + 1 >= genotype.length) return false
            val dominant = genotype[i]
            val recessive = genotype[i + 1]

            if (!dominant.isUpperCase() || !recessive.isLowerCase()) return false
            if (dominant.lowercaseChar() != recessive) return false
        }

        return true
    }

    private fun displayResults(result: PunnettResult) {
        binding.apply {
            // Show cards
            resultsCard.visibility = View.VISIBLE
            gridCard.visibility = View.VISIBLE

            // Setup grid
            val gridSize = when (currentCrossType) {
                CrossType.MONOHYBRID -> 2
                CrossType.DIHYBRID -> 4
                CrossType.TRIHYBRID -> 8
            }

            punnettGridRV.layoutManager = GridLayoutManager(context, gridSize + 1) // +1 for headers
            punnettAdapter = PunnettSquareAdapter(result.grid, result.parent1Gametes, result.parent2Gametes)
            punnettGridRV.adapter = punnettAdapter

            // Display ratios
            displayRatios(result)
        }
    }

    private fun displayRatios(result: PunnettResult) {
        binding.apply {
            // Build genotypic ratio text
            val genotypicRatioText = buildString {
                append("Genotypic Ratio:\n")
                result.genotypicRatio.entries.forEachIndexed { index, entry ->
                    append("• ${entry.key}: ${entry.value} (${String.format("%.1f", result.genotypicProbabilities[entry.key])}%)")
                    if (index < result.genotypicRatio.size - 1) append("\n")
                }
            }
            tvGenotypicRatio.text = genotypicRatioText

            // Build phenotypic ratio text
            val phenotypicRatioText = buildString {
                append("Phenotypic Ratio:\n")
                result.phenotypicRatio.entries.forEachIndexed { index, entry ->
                    append("• ${entry.key}: ${entry.value} (${String.format("%.1f", result.phenotypicProbabilities[entry.key])}%)")
                    if (index < result.phenotypicRatio.size - 1) append("\n")
                }
            }
            tvPhenotypicRatio.text = phenotypicRatioText

            // Display simplified ratio
            val simplifiedRatio = simplifyRatio(result.genotypicRatio.values.toList())
            tvSimplifiedRatio.text = "Ratio: ${simplifiedRatio.joinToString(":")}"
        }
    }

    private fun simplifyRatio(numbers: List<Int>): List<Int> {
        if (numbers.isEmpty()) return emptyList()
        val gcd = numbers.reduce { a, b -> gcd(a, b) }
        return numbers.map { it / gcd }
    }

    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    private val parentTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // Auto-calculate disabled for performance, user must click Calculate
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPunnettSquareBinding.inflate(inflater, container, false)
        return binding.root
    }

    enum class CrossType {
        MONOHYBRID,
        DIHYBRID,
        TRIHYBRID
    }
}

// Calculator class
class PunnettSquareCalculator {

    fun calculate(parent1: String, parent2: String, crossType: PunnettSquareFragment.CrossType): PunnettResult {
        val parent1Gametes = generateGametes(parent1)
        val parent2Gametes = generateGametes(parent2)

        val grid = createGrid(parent1Gametes, parent2Gametes)
        val offspring = grid.flatten()

        val genotypicRatio = countGenotypes(offspring)
        val genotypicProbabilities = calculateProbabilities(genotypicRatio, offspring.size)

        val phenotypicRatio = countPhenotypes(offspring)
        val phenotypicProbabilities = calculateProbabilities(phenotypicRatio, offspring.size)

        return PunnettResult(
            grid = grid,
            parent1Gametes = parent1Gametes,
            parent2Gametes = parent2Gametes,
            genotypicRatio = genotypicRatio,
            genotypicProbabilities = genotypicProbabilities,
            phenotypicRatio = phenotypicRatio,
            phenotypicProbabilities = phenotypicProbabilities
        )
    }

    private fun generateGametes(genotype: String): List<String> {
        // Split genotype into trait pairs: "AaBb" -> ["Aa", "Bb"]
        val traits = genotype.chunked(2)

        // Generate all combinations
        return generateCombinations(traits)
    }

    private fun generateCombinations(traits: List<String>): List<String> {
        if (traits.isEmpty()) return listOf("")

        val firstTrait = traits[0]
        val remainingTraits = traits.drop(1)
        val remainingCombinations = generateCombinations(remainingTraits)

        val result = mutableListOf<String>()
        for (allele in firstTrait) {
            for (combo in remainingCombinations) {
                result.add("$allele$combo")
            }
        }

        return result
    }

    private fun createGrid(parent1Gametes: List<String>, parent2Gametes: List<String>): List<List<String>> {
        val grid = mutableListOf<List<String>>()

        for (gamete2 in parent2Gametes) {
            val row = mutableListOf<String>()
            for (gamete1 in parent1Gametes) {
                row.add(combineGametes(gamete1, gamete2))
            }
            grid.add(row)
        }

        return grid
    }

    private fun combineGametes(gamete1: String, gamete2: String): String {
        // Combine and sort alleles properly
        val combined = mutableListOf<String>()

        for (i in gamete1.indices) {
            val allele1 = gamete1[i]
            val allele2 = gamete2[i]

            // Put dominant (uppercase) first
            val pair = if (allele1.isUpperCase() || allele2.isLowerCase()) {
                "$allele1$allele2"
            } else {
                "$allele2$allele1"
            }

            // Sort within pair: uppercase first
            val sorted = pair.toList().sortedByDescending { it.isUpperCase() }.joinToString("")
            combined.add(sorted)
        }

        return combined.joinToString("")
    }

    private fun countGenotypes(offspring: List<String>): Map<String, Int> {
        return offspring.groupingBy { it }.eachCount().toSortedMap()
    }

    private fun countPhenotypes(offspring: List<String>): Map<String, Int> {
        return offspring.groupingBy { getPhenotype(it) }.eachCount()
    }

    private fun getPhenotype(genotype: String): String {
        // Count dominant vs recessive traits
        val traits = genotype.chunked(2)
        val dominantCount = traits.count { it[0].isUpperCase() }

        return when (dominantCount) {
            traits.size -> "All Dominant"
            0 -> "All Recessive"
            else -> "$dominantCount Dominant, ${traits.size - dominantCount} Recessive"
        }
    }

    private fun calculateProbabilities(counts: Map<String, Int>, total: Int): Map<String, Double> {
        return counts.mapValues { (it.value.toDouble() / total) * 100 }
    }
}

// Data class
data class PunnettResult(
    val grid: List<List<String>>,
    val parent1Gametes: List<String>,
    val parent2Gametes: List<String>,
    val genotypicRatio: Map<String, Int>,
    val genotypicProbabilities: Map<String, Double>,
    val phenotypicRatio: Map<String, Int>,
    val phenotypicProbabilities: Map<String, Double>
)

// Adapter for grid
class PunnettSquareAdapter(
    private val grid: List<List<String>>,
    private val parent1Gametes: List<String>,
    private val parent2Gametes: List<String>
) : androidx.recyclerview.widget.RecyclerView.Adapter<PunnettSquareAdapter.ViewHolder>() {

    private val gridSize = grid.size
    private val totalItems = (gridSize + 1) * (gridSize + 1) // +1 for headers

    class ViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        view.layoutParams.height = 120
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = position / (gridSize + 1)
        val col = position % (gridSize + 1)

        holder.textView.apply {
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setPadding(8, 8, 8, 8)

            when {
                row == 0 && col == 0 -> {
                    // Top-left corner (empty)
                    text = ""
                    setBackgroundColor(Color.parseColor("#424242"))
                    setTextColor(Color.WHITE)
                }
                row == 0 -> {
                    // Top row headers (parent 1 gametes)
                    text = parent1Gametes[col - 1]
                    setBackgroundColor(Color.parseColor("#616161"))
                    setTextColor(Color.WHITE)
                    textSize = 16f
                }
                col == 0 -> {
                    // Left column headers (parent 2 gametes)
                    text = parent2Gametes[row - 1]
                    setBackgroundColor(Color.parseColor("#616161"))
                    setTextColor(Color.WHITE)
                    textSize = 16f
                }
                else -> {
                    // Grid cells
                    val genotype = grid[row - 1][col - 1]
                    text = genotype
                    setBackgroundColor(getGenotypeColor(genotype))
                    setTextColor(Color.BLACK)
                    textSize = 13f
                }
            }
        }
    }

    override fun getItemCount() = totalItems

    private fun getGenotypeColor(genotype: String): Int {
        val dominantCount = genotype.count { it.isUpperCase() }
        val totalAlleles = genotype.length

        return when {
            dominantCount == totalAlleles -> Color.parseColor("#FF6B6B") // All dominant - Red
            dominantCount == 0 -> Color.parseColor("#4ECDC4") // All recessive - Cyan
            dominantCount > totalAlleles / 2 -> Color.parseColor("#FFD93D") // Mostly dominant - Yellow
            else -> Color.parseColor("#95E1D3") // Mostly recessive - Light green
        }
    }
}