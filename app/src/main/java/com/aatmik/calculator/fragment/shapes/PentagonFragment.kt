package com.aatmik.calculator.fragment.shapes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentPentagonBinding
import kotlin.math.pow
import kotlin.math.sqrt

class PentagonFragment : Fragment() {

    private var _binding: FragmentPentagonBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPentagonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.calculateBtn.setOnClickListener {
            calculatePentagon()
        }
    }

    private fun calculatePentagon() {
        val sideLength = binding.sideEt.text.toString().toDoubleOrNull()

        if (sideLength == null || sideLength <= 0) {
            binding.sideInputLayout.error = "Please enter a valid side length"
            return
        } else {
            binding.sideInputLayout.error = null
        }

        val area = calculateArea(sideLength)
        val perimeter = calculatePerimeter(sideLength)
        val inradius = calculateInradius(sideLength)
        val circumradius = calculateCircumradius(sideLength)
        val centralAngle = calculateCentralAngle()
        val interiorAngle = calculateInteriorAngle()

        binding.areaTv.text = "Area: ${String.format("%.2f", area)}"
        binding.perimeterTv.text = "Perimeter: ${String.format("%.2f", perimeter)}"
        binding.inradiusTv.text = "Inradius: ${String.format("%.2f", inradius)}"
        binding.circumradiusTv.text = "Circumradius: ${String.format("%.2f", circumradius)}"
        binding.centralAngleTv.text = "Central Angle: ${String.format("%.2f", centralAngle)}°"
        binding.interiorAngleTv.text = "Interior Angle: ${String.format("%.2f", interiorAngle)}°"
    }

    private fun calculateArea(side: Double): Double {
        return 0.25 * sqrt(5 * (5 + 2 * sqrt(5.0))) * side.pow(2)
    }

    private fun calculatePerimeter(side: Double): Double {
        return 5 * side
    }

    private fun calculateInradius(side: Double): Double {
        return side * (sqrt(25 + 10 * sqrt(5.0))) / 10
    }

    private fun calculateCircumradius(side: Double): Double {
        return side * sqrt(10 + 2 * sqrt(5.0)) / 4
    }

    private fun calculateCentralAngle(): Double {
        return 360.0 / 5
    }

    private fun calculateInteriorAngle(): Double {
        return (5 - 2) * 180.0 / 5
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}