package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentTriangleBinding
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

class TriangleFragment : Fragment() {

    private var _binding: FragmentTriangleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTriangleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateTriangleProperties()
        }
    }

    private fun calculateTriangleProperties() {
        val a = binding.sideAEt.text.toString().toDoubleOrNull()
        val b = binding.sideBEt.text.toString().toDoubleOrNull()
        val c = binding.sideCEt.text.toString().toDoubleOrNull()

        if (a != null && b != null && c != null) {
            val s = (a + b + c) / 2 // Semi-perimeter
            val area = sqrt(s * (s - a) * (s - b) * (s - c))
            val perimeter = a + b + c

            // Calculate angles using the law of cosines
            val angleA = acos((b * b + c * c - a * a) / (2 * b * c)) * (180 / PI)
            val angleB = acos((a * a + c * c - b * b) / (2 * a * c)) * (180 / PI)
            val angleC = 180 - angleA - angleB

            val inradius = area / s
            val circumradius = (a * b * c) / (4 * area)

            binding.areaTv.text = "Area: %.2f".format(area)
            binding.perimeterTv.text = "Perimeter: %.2f".format(perimeter)
            // binding.anglesTv.text = "Angles: A=%.2f°, B=%.2f°, C=%.2f°".format(angleA, angle
        }
    }
}