package com.aatmik.calculator.fragment.shapes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentHexagonBinding
import kotlin.math.pow
import kotlin.math.sqrt

class HexagonFragment : Fragment() {

    private var _binding: FragmentHexagonBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHexagonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.calculateBtn.setOnClickListener {
            calculateHexagon()
        }
    }

    private fun calculateHexagon() {
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
        return 3 * sqrt(3.0) * side.pow(2) / 2
    }

    private fun calculatePerimeter(side: Double): Double {
        return 6 * side
    }

    private fun calculateInradius(side: Double): Double {
        return side * sqrt(3.0) / 2
    }

    private fun calculateCircumradius(side: Double): Double {
        return side
    }

    private fun calculateCentralAngle(): Double {
        return 360.0 / 6
    }

    private fun calculateInteriorAngle(): Double {
        return (6 - 2) * 180.0 / 6
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}