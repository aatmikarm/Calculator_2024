package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentRectangleBinding
import kotlin.math.sqrt

class RectangleFragment : Fragment() {

    private var _binding: FragmentRectangleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRectangleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateRectangleProperties()
        }
    }

    private fun calculateRectangleProperties() {
        val length = binding.lengthEt.text.toString().toDoubleOrNull()
        val width = binding.widthEt.text.toString().toDoubleOrNull()

        if (length != null && width != null) {
            val area = length * width
            val perimeter = 2 * (length + width)
            val diagonal = sqrt(length * length + width * width)
            val volume = length * width * 1 // Assuming height is 1 unit
            val aspectRatio = length / width

            binding.areaTv.text = "Area: %.2f".format(area)
            binding.perimeterTv.text = "Perimeter: %.2f".format(perimeter)
            binding.diagonalTv.text = "Diagonal: %.2f".format(diagonal)
            binding.volumeTv.text = "Volume (height=1): %.2f".format(volume)
            binding.aspectRatioTv.text = "Aspect Ratio: %.2f".format(aspectRatio)
        } else {
            // Handle invalid input
            binding.areaTv.text = "Invalid input"
            binding.perimeterTv.text = ""
            binding.diagonalTv.text = ""
            binding.volumeTv.text = ""
            binding.aspectRatioTv.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

    }
}