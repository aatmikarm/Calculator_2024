package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentPercentageBinding

class PercentageFragment : Fragment() {

    private lateinit var binding: FragmentPercentageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentPercentageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    companion object {
        private const val TAG = "PercentageFragment"
    }
}