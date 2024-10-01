package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentEllipsoidBinding

class EllipsoidFragment : Fragment() {

    private var _binding: FragmentEllipsoidBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEllipsoidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateEllipsoidProperties()
        }
    }

    private fun calculateEllipsoidProperties() {
        val semiMajorAxis = binding.semiMajorAxisEt.text.toString().toDoubleOrNull()
        val semiMinorAxis = binding.semiMinorAxisEt.text.toString().toDoubleOrNull()

        if (semiMajorAxis != null && semiMinorAxis != null) {
            // Calculate volume and surface area
            val volume = (4.0 / 3.0) * Math.PI * semiMajorAxis * semiMinorAxis * semiMinorAxis * semiMinorAxis / 3
            val surfaceArea = 4 * Math.PI * Math.pow((Math.pow(semiMajorAxis, 1.6) + Math.pow(semiMinorAxis, 1.6)) / 2, 1 / 1.6)

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
