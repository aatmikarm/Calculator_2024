package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentSquareBinding
import kotlin.math.sqrt

class SquareFragment : Fragment() {

    private var _binding: FragmentSquareBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSquareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateSquareProperties()
        }
    }

    private fun calculateSquareProperties() {
        val side = binding.sideEt.text.toString().toDoubleOrNull()

        if (side != null) {
            val area = side * side
            val perimeter = 4 * side
            val diagonal = side * sqrt(2.0)
            val volume = side * side * side
            val surfaceArea = 6 * side * side

            binding.areaTv.text = "Area: %.2f".format(area)
            binding.perimeterTv.text = "Perimeter: %.2f".format(perimeter)
            binding.diagonalTv.text = "Diagonal: %.2f".format(diagonal)
            binding.volumeTv.text = "Volume (cube): %.2f".format(volume)
            binding.surfaceAreaTv.text = "Surface Area (cube): %.2f".format(surfaceArea)
        } else {
            // Handle invalid input
            binding.areaTv.text = "Invalid input"
            binding.perimeterTv.text = ""
            binding.diagonalTv.text = ""
            binding.volumeTv.text = ""
            binding.surfaceAreaTv.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SquareFragment()
    }
}