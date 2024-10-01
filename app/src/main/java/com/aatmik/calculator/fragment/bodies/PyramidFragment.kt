package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentPyramidBinding
import kotlin.math.pow

class PyramidFragment : Fragment() {

    private var _binding: FragmentPyramidBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPyramidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculatePyramidProperties()
        }

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculatePyramidProperties() {
        val baseLength = binding.baseLengthEt.text.toString().toDoubleOrNull()
        val height = binding.heightEt.text.toString().toDoubleOrNull()

        if (baseLength != null && height != null) {
            // Calculate volume and surface area
            val volume = (1.0 / 3.0) * baseLength.pow(2) * height
            val slantHeight = (baseLength / 2).pow(2) + height.pow(2)
            val surfaceArea = baseLength.pow(2) + (baseLength * slantHeight)

            binding.volumeTv.text = "Volume: %.2f".format(volume)
            binding.surfaceAreaTv.text = "Surface Area: %.2f".format(surfaceArea)
        } else {
            // Handle invalid input
            binding.volumeTv.text = "Invalid input"
            binding.surfaceAreaTv.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Optional: Add any necessary constants or methods here
    }
}
