package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCircleBinding
import kotlin.math.PI
import kotlin.math.pow

class CircleFragment : Fragment() {

    private var _binding: FragmentCircleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCircleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculateBtn.setOnClickListener {
            calculateCircleProperties()
        }
    }

    private fun calculateCircleProperties() {
        val radius = binding.radiusEt.text.toString().toDoubleOrNull()
        if (radius != null) {
            val area = PI * radius.pow(2)
            val circumference = 2 * PI * radius
            val diameter = 2 * radius
            val sphereVolume = (4.0 / 3.0) * PI * radius.pow(3)
            val sphereSurfaceArea = 4 * PI * radius.pow(2)

            binding.areaTv.text = "Area: %.2f".format(area)
            binding.circumferenceTv.text = "Circumference: %.2f".format(circumference)
            binding.diameterTv.text = "Diameter: %.2f".format(diameter)
            binding.sphereVolumeTv.text = "Sphere Volume: %.2f".format(sphereVolume)
            binding.sphereSurfaceAreaTv.text = "Sphere Surface Area: %.2f".format(sphereSurfaceArea)
        } else {
            // Handle invalid input
            binding.areaTv.text = "Invalid input"
            binding.circumferenceTv.text = ""
            binding.diameterTv.text = ""
            binding.sphereVolumeTv.text = ""
            binding.sphereSurfaceAreaTv.text = ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

    }
}