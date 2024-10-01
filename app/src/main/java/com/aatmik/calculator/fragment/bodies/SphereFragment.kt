package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentSphereBinding
import kotlin.math.PI
import kotlin.math.pow

class SphereFragment : Fragment() {

    private var _binding: FragmentSphereBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSphereBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateSphereProperties()
        }
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun calculateSphereProperties() {
        val radius = binding.radiusEt.text.toString().toDoubleOrNull()
        if (radius != null) {
            val volume = (4.0 / 3.0) * PI * radius.pow(3)
            val surfaceArea = 4 * PI * radius.pow(2)

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
        // You can add companion object methods or properties if needed
    }
}
