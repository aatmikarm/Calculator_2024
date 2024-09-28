package com.aatmik.calculator.fragment

import android.animation.ArgbEvaluator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentTipBinding

class TipFragment : Fragment() {

    private lateinit var binding: FragmentTipBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }


            seekBarTip.progress = INITIAL_TIP_PERCENT
            tvTipPercentLabel.text = "$INITIAL_TIP_PERCENT%"
            updateTipDescription(INITIAL_TIP_PERCENT)
            seekBarTip.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    Log.i(TAG, "onProgressChanged $progress")
                    tvTipPercentLabel.text = "$progress%"
                    computeTipAndTotal()
                    updateTipDescription(progress)

                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}

                override fun onStopTrackingTouch(p0: SeekBar?) {}

            })
            with(etBaseAmount) {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        Log.i(TAG, "afterTextChanged $s")
                        computeTipAndTotal()
                    }

                })
            }
        }


    }

    private fun updateTipDescription(tipPercent: Int) {
        binding.apply {
            val tipDescription = when (tipPercent) {
                in 0..9 -> "Poor"
                in 10..14 -> "Acceptable"
                in 15..19 -> "Good"
                in 20..24 -> "Great"
                else -> "Amazing"
            }
            tvTipDescription.text = tipDescription
            //Update the color based on the tipPercent
            val color = ArgbEvaluator().evaluate(
                tipPercent.toFloat() / seekBarTip.max,
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            ) as Int
            tvTipDescription.setTextColor(color)
        }


    }

    private fun computeTipAndTotal() {
        binding.apply {
            if (etBaseAmount.text.isEmpty()) {
                tvTipAmount.text = ""
                tvTotalAmount.text = ""
                return
            }
            //1. Get the value of the base and tip percent
            val baseAmount = etBaseAmount.text.toString().toDouble()
            val tipPercent = seekBarTip.progress
            //2. Compute the tip and total
            val tipAmount = baseAmount * tipPercent / 100
            val totalAmount = baseAmount + tipAmount
            //3. Update the UI
            tvTipAmount.text = "%.2f".format(tipAmount)
            tvTotalAmount.text = "%.2f".format(totalAmount)
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTipBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val INITIAL_TIP_PERCENT = 15
        private const val TAG = "TipFragment"
    }
}