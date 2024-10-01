package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentTetrahedronBinding
import kotlin.math.pow

class TetrahedronFragment : Fragment() {

    private var _binding: FragmentTetrahedronBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTetrahedronBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateTetrahedronProperties()
        }

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculateTetrahedronProperties() {
        val sideLength = binding.sideEt.text.toString().toDoubleOrNull()

        if (sideLength != null) {
            // Calculate volume and surface area
            val volume = (sideLength.pow(3) / (6 * Math.sqrt(2.0))).toString()
            val surfaceArea = (Math.sqrt(3.0) * sideLength.pow(2)).toString()

            binding.volumeTv.text = "Volume: %.2f".format(volume.toDouble())
            binding.surfaceAreaTv.text = "Surface Area: %.2f".format(surfaceArea.toDouble())
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
