package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentRectangularPrismBinding

class RectangularPrismFragment : Fragment() {

    private var _binding: FragmentRectangularPrismBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRectangularPrismBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateRectangularPrismProperties()
        }
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculateRectangularPrismProperties() {
        val length = binding.lengthEt.text.toString().toDoubleOrNull()
        val width = binding.widthEt.text.toString().toDoubleOrNull()
        val height = binding.heightEt.text.toString().toDoubleOrNull()

        if (length != null && width != null && height != null) {
            // Calculate volume and surface area
            val volume = length * width * height
            val surfaceArea = 2 * (length * width + width * height + length * height)

            binding.volumeTv.text = "Volume: %.2f".format(volume)
            binding.surfaceAreaTv.text = "Surface Area: %.2f".format(surfaceArea)
        } else {
            // Handle invalid input
            binding.volumeTv.text = "Please enter valid numbers"
            binding.surfaceAreaTv.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
