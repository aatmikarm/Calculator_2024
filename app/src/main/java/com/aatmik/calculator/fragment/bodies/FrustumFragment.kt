package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentFrustumBinding
import kotlin.math.pow
import kotlin.math.sqrt

class FrustumFragment : Fragment() {

    private var _binding: FragmentFrustumBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFrustumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateFrustumProperties()
        }

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculateFrustumProperties() {
        val topRadius = binding.topRadiusEt.text.toString().toDoubleOrNull()
        val bottomRadius = binding.bottomRadiusEt.text.toString().toDoubleOrNull()
        val height = binding.heightEt.text.toString().toDoubleOrNull()

        if (topRadius != null && bottomRadius != null && height != null) {
            // Calculate volume and surface area
            val volume =
                (1.0 / 3.0) * height * (Math.PI * (topRadius.pow(2) + bottomRadius.pow(2) + topRadius * bottomRadius))
            val slantHeight = sqrt(height.pow(2) + (bottomRadius - topRadius).pow(2))
            val surfaceArea =
                (Math.PI * (topRadius + bottomRadius) * slantHeight) + (Math.PI * topRadius.pow(2)) + (Math.PI * bottomRadius.pow(
                    2
                ))

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
