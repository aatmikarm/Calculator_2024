package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.FragmentGpaCalculatorBinding

class GpaCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentGpaCalculatorBinding
    private var selectedGradingSystem: GradingSystem = GradingSystem.US_4_POINT
    private val subjectsList = mutableListOf<Subject>()
    private lateinit var subjectsAdapter: SubjectsAdapter

    // Subject data class
    data class Subject(
        val name: String,
        val credits: Double,
        val grade: String,
        val gradePoint: Double
    )

    // Grading system data class
    data class GradingSystem(
        val name: String,
        val maxGpa: Double,
        val gradeScale: Map<String, Double>,
        val percentageToGrade: (Double) -> String?,
        val description: String
    ) {
        companion object {
            val US_4_POINT = GradingSystem(
                "US 4.0 Scale",
                4.0,
                mapOf(
                    "A+" to 4.0, "A" to 4.0, "A-" to 3.7,
                    "B+" to 3.3, "B" to 3.0, "B-" to 2.7,
                    "C+" to 2.3, "C" to 2.0, "C-" to 1.7,
                    "D+" to 1.3, "D" to 1.0, "F" to 0.0
                ),
                { percentage ->
                    when {
                        percentage >= 97 -> "A+"
                        percentage >= 93 -> "A"
                        percentage >= 90 -> "A-"
                        percentage >= 87 -> "B+"
                        percentage >= 83 -> "B"
                        percentage >= 80 -> "B-"
                        percentage >= 77 -> "C+"
                        percentage >= 73 -> "C"
                        percentage >= 70 -> "C-"
                        percentage >= 67 -> "D+"
                        percentage >= 60 -> "D"
                        else -> "F"
                    }
                },
                "Standard US university system"
            )

            val INDIAN_10_POINT = GradingSystem(
                "Indian 10.0 Scale (CGPA)",
                10.0,
                mapOf(
                    "O" to 10.0, "A+" to 9.0, "A" to 8.0, "B+" to 7.0,
                    "B" to 6.0, "C" to 5.0, "P" to 4.0, "F" to 0.0
                ),
                { percentage ->
                    when {
                        percentage >= 90 -> "O"
                        percentage >= 80 -> "A+"
                        percentage >= 70 -> "A"
                        percentage >= 60 -> "B+"
                        percentage >= 50 -> "B"
                        percentage >= 40 -> "C"
                        percentage >= 35 -> "P"
                        else -> "F"
                    }
                },
                "Indian universities CGPA system"
            )

            val UK_DEGREE = GradingSystem(
                "UK Degree Classification",
                4.0,
                mapOf(
                    "First Class" to 4.0, "Upper Second" to 3.3,
                    "Lower Second" to 2.7, "Third Class" to 2.0, "Fail" to 0.0
                ),
                { percentage ->
                    when {
                        percentage >= 70 -> "First Class"
                        percentage >= 60 -> "Upper Second"
                        percentage >= 50 -> "Lower Second"
                        percentage >= 40 -> "Third Class"
                        else -> "Fail"
                    }
                },
                "British university honors degree"
            )

            val CANADA_4_POINT = GradingSystem(
                "Canadian 4.0 Scale",
                4.0,
                mapOf(
                    "A+" to 4.0, "A" to 4.0, "A-" to 3.7,
                    "B+" to 3.3, "B" to 3.0, "B-" to 2.7,
                    "C+" to 2.3, "C" to 2.0, "C-" to 1.7,
                    "D+" to 1.3, "D" to 1.0, "F" to 0.0
                ),
                { percentage ->
                    when {
                        percentage >= 90 -> "A+"
                        percentage >= 85 -> "A"
                        percentage >= 80 -> "A-"
                        percentage >= 77 -> "B+"
                        percentage >= 73 -> "B"
                        percentage >= 70 -> "B-"
                        percentage >= 67 -> "C+"
                        percentage >= 63 -> "C"
                        percentage >= 60 -> "C-"
                        percentage >= 57 -> "D+"
                        percentage >= 50 -> "D"
                        else -> "F"
                    }
                },
                "Canadian university system"
            )

            val AUSTRALIA_7_POINT = GradingSystem(
                "Australian 7.0 Scale",
                7.0,
                mapOf(
                    "HD" to 7.0, "D" to 6.0, "C" to 5.0,
                    "P" to 4.0, "F" to 0.0
                ),
                { percentage ->
                    when {
                        percentage >= 85 -> "HD"  // High Distinction
                        percentage >= 75 -> "D"   // Distinction
                        percentage >= 65 -> "C"   // Credit
                        percentage >= 50 -> "P"   // Pass
                        else -> "F"               // Fail
                    }
                },
                "Australian university system"
            )

            val GERMAN_6_POINT = GradingSystem(
                "German 6.0 Scale",
                1.0, // Note: German system is inverted (1.0 is best)
                mapOf(
                    "1.0" to 1.0, "1.3" to 1.3, "1.7" to 1.7,
                    "2.0" to 2.0, "2.3" to 2.3, "2.7" to 2.7,
                    "3.0" to 3.0, "3.3" to 3.3, "3.7" to 3.7,
                    "4.0" to 4.0, "5.0" to 5.0, "6.0" to 6.0
                ),
                { percentage ->
                    when {
                        percentage >= 95 -> "1.0"
                        percentage >= 90 -> "1.3"
                        percentage >= 85 -> "1.7"
                        percentage >= 80 -> "2.0"
                        percentage >= 75 -> "2.3"
                        percentage >= 70 -> "2.7"
                        percentage >= 65 -> "3.0"
                        percentage >= 60 -> "3.3"
                        percentage >= 55 -> "3.7"
                        percentage >= 50 -> "4.0"
                        else -> "5.0"
                    }
                },
                "German university system (lower is better)"
            )
        }
    }

    private val gradingSystems = listOf(
        GradingSystem.US_4_POINT,
        GradingSystem.INDIAN_10_POINT,
        GradingSystem.UK_DEGREE,
        GradingSystem.CANADA_4_POINT,
        GradingSystem.AUSTRALIA_7_POINT,
        GradingSystem.GERMAN_6_POINT
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set up grading system spinner
            setupGradingSystemSpinner()

            // Set up RecyclerView
            setupRecyclerView()

            // Add subject button
            btnAddSubject.setOnClickListener {
                addSubject()
            }

            // Clear all button
            btnClearAll.setOnClickListener {
                clearAllSubjects()
            }

            // Calculate GPA button
            btnCalculateGpa.setOnClickListener {
                calculateGpa()
            }

            // Set up text watchers for real-time validation
            etSubjectName.addTextChangedListener(inputTextWatcher)
            etCredits.addTextChangedListener(inputTextWatcher)
            etGrade.addTextChangedListener(inputTextWatcher)
        }
    }

    private fun setupGradingSystemSpinner() {
        val systemNames = gradingSystems.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, systemNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerGradingSystem.adapter = adapter
        binding.spinnerGradingSystem.setSelection(0) // Default to US system

        binding.spinnerGradingSystem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGradingSystem = gradingSystems[position]
                updateGradeInputHint()
                calculateGpa() // Recalculate with new system
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRecyclerView() {
        subjectsAdapter = SubjectsAdapter(subjectsList) { position ->
            removeSubject(position)
        }
        binding.rvSubjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subjectsAdapter
        }
    }

    private fun updateGradeInputHint() {
        val hint = when (selectedGradingSystem.name) {
            "German 6.0 Scale" -> "Grade (1.0-6.0) or %"
            "Indian 10.0 Scale (CGPA)" -> "Grade (O,A+,A,B+,B,C,P,F) or %"
            "UK Degree Classification" -> "Classification or %"
            else -> "Grade (A+,A,B+,etc) or %"
        }
        binding.gradeInputLayout.hint = hint
    }

    private fun addSubject() {
        binding.apply {
            val subjectName = etSubjectName.text.toString().trim()
            val creditsText = etCredits.text.toString().trim()
            val gradeText = etGrade.text.toString().trim().uppercase()

            // Validation
            if (subjectName.isEmpty()) {
                etSubjectName.error = "Enter subject name"
                return
            }

            if (creditsText.isEmpty()) {
                etCredits.error = "Enter credits"
                return
            }

            val credits = creditsText.toDoubleOrNull()
            if (credits == null || credits <= 0) {
                etCredits.error = "Enter valid credits"
                return
            }

            if (gradeText.isEmpty()) {
                etGrade.error = "Enter grade or percentage"
                return
            }

            // Convert grade/percentage to grade point
            val gradePoint = convertToGradePoint(gradeText)
            if (gradePoint == null) {
                etGrade.error = "Invalid grade format"
                return
            }

            // Add subject to list
            val subject = Subject(subjectName, credits, gradeText, gradePoint)
            subjectsList.add(subject)
            subjectsAdapter.notifyItemInserted(subjectsList.size - 1)

            // Clear input fields
            etSubjectName.text?.clear()
            etCredits.text?.clear()
            etGrade.text?.clear()

            // Calculate GPA
            calculateGpa()

            Toast.makeText(requireContext(), "Subject added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertToGradePoint(input: String): Double? {
        // Try to parse as percentage first
        val percentage = input.replace("%", "").toDoubleOrNull()
        if (percentage != null && percentage in 0.0..100.0) {
            val grade = selectedGradingSystem.percentageToGrade(percentage)
            return grade?.let { selectedGradingSystem.gradeScale[it] }
        }

        // Try to parse as direct grade
        return selectedGradingSystem.gradeScale[input]
    }

    private fun removeSubject(position: Int) {
        subjectsList.removeAt(position)
        subjectsAdapter.notifyItemRemoved(position)
        calculateGpa()
        Toast.makeText(requireContext(), "Subject removed", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllSubjects() {
        subjectsList.clear()
        subjectsAdapter.notifyDataSetChanged()
        binding.apply {
            tvGpaResult.text = "Add subjects to calculate GPA"
            tvPercentageResult.text = ""
            tvGradeResult.text = ""
            tvClassificationResult.text = ""
        }
        Toast.makeText(requireContext(), "All subjects cleared", Toast.LENGTH_SHORT).show()
    }

    private fun calculateGpa() {
        binding.apply {
            if (subjectsList.isEmpty()) {
                tvGpaResult.text = "Add subjects to calculate GPA"
                tvPercentageResult.text = ""
                tvGradeResult.text = ""
                tvClassificationResult.text = ""
                return
            }

            var totalPoints = 0.0
            var totalCredits = 0.0

            subjectsList.forEach { subject ->
                totalPoints += subject.gradePoint * subject.credits
                totalCredits += subject.credits
            }

            val gpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0

            // Convert GPA to percentage (approximate)
            val percentage = when (selectedGradingSystem.name) {
                "German 6.0 Scale" -> (6.0 - gpa) / 5.0 * 100 // Inverted scale
                "Indian 10.0 Scale (CGPA)" -> gpa * 9.5 // Common conversion
                else -> (gpa / selectedGradingSystem.maxGpa) * 100
            }

            // Get overall grade
            val overallGrade = selectedGradingSystem.percentageToGrade(percentage)

            // Get classification
            val classification = getClassification(gpa, percentage)

            // Display results
            tvGpaResult.text = String.format("GPA: %.2f / %.1f", gpa, selectedGradingSystem.maxGpa)
            tvPercentageResult.text = "Equivalent: ${String.format("%.1f", percentage)}%"
            tvGradeResult.text = "Grade: ${overallGrade ?: "N/A"}"
            tvClassificationResult.text = classification
        }
    }

    private fun getClassification(gpa: Double, percentage: Double): String {
        return when (selectedGradingSystem.name) {
            "US 4.0 Scale", "Canadian 4.0 Scale" -> when {
                gpa >= 3.8 -> "Summa Cum Laude (Highest Honors)"
                gpa >= 3.5 -> "Magna Cum Laude (High Honors)"
                gpa >= 3.2 -> "Cum Laude (Honors)"
                gpa >= 2.0 -> "Good Standing"
                else -> "Below Standards"
            }
            "Indian 10.0 Scale (CGPA)" -> when {
                gpa >= 8.5 -> "First Class with Distinction"
                gpa >= 7.0 -> "First Class"
                gpa >= 6.0 -> "Second Class"
                gpa >= 5.0 -> "Pass Class"
                else -> "Fail"
            }
            "UK Degree Classification" -> when {
                percentage >= 70 -> "First Class Honours"
                percentage >= 60 -> "Upper Second Class Honours (2:1)"
                percentage >= 50 -> "Lower Second Class Honours (2:2)"
                percentage >= 40 -> "Third Class Honours"
                else -> "Fail"
            }
            "Australian 7.0 Scale" -> when {
                gpa >= 6.0 -> "High Distinction"
                gpa >= 5.5 -> "Distinction"
                gpa >= 4.5 -> "Credit"
                gpa >= 4.0 -> "Pass"
                else -> "Fail"
            }
            "German 6.0 Scale" -> when {
                gpa <= 1.5 -> "Sehr Gut (Very Good)"
                gpa <= 2.5 -> "Gut (Good)"
                gpa <= 3.5 -> "Befriedigend (Satisfactory)"
                gpa <= 4.0 -> "Ausreichend (Sufficient)"
                else -> "Nicht Bestanden (Fail)"
            }
            else -> "Good Academic Standing"
        }
    }

    // TextWatcher for input validation
    private val inputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // Clear any existing errors when user starts typing
            binding.apply {
                etSubjectName.error = null
                etCredits.error = null
                etGrade.error = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGpaCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "GpaCalculatorFragment"
    }

    // RecyclerView Adapter for subjects
    inner class SubjectsAdapter(
        private val subjects: MutableList<Subject>,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder>() {

        inner class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(subject: Subject, position: Int) {
                val text1 = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
                val text2 = itemView.findViewById<android.widget.TextView>(android.R.id.text2)

                text1.text = "${subject.name} (${subject.credits} credits)"
                text2.text = "Grade: ${subject.grade} • Points: ${String.format("%.2f", subject.gradePoint)}"

                // Set click listener for deletion with visual feedback
                itemView.setOnClickListener {
                    onDeleteClick(position)
                }

                // Add some padding for better appearance
                itemView.setPadding(16, 12, 16, 12)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return SubjectViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
            val subject = subjects[position]
            holder.bind(subject, position)
        }

        override fun getItemCount() = subjects.size
    }
}