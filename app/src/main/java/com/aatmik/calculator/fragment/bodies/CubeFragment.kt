package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCubeBinding
import kotlin.math.pow

class CubeFragment : Fragment() {

    private var _binding: FragmentCubeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCubeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateCubeProperties()
        }

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculateCubeProperties() {
        val sideLength =
            binding.sideEt.text.toString().toDoubleOrNull() // Use the input field for side length
        if (sideLength != null) {
            // Calculate properties of the cube
            val surfaceArea = 6 * sideLength.pow(2) // Surface Area = 6 * side²
            val volume = sideLength.pow(3) // Volume = side³
            val diagonal = sideLength * Math.sqrt(3.0) // Diagonal = side * √3

            // Display the results in the corresponding TextViews
            binding.areaTv.text = "Surface Area: %.2f".format(surfaceArea)
            binding.volumeTv.text =
                "Volume: %.2f".format(volume) // Make sure to add this TextView to your layout
        } else {
            // Handle invalid input
            binding.areaTv.text = "Invalid input"
            binding.volumeTv.text = ""
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

    }
}