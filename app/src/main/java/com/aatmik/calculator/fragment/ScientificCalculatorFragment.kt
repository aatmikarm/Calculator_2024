package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentScientificCalculatorBinding
import com.aatmik.calculator.util.ButtonUtil.addNumberValueToText
import com.aatmik.calculator.util.ButtonUtil.addOperatorValueToText
import com.aatmik.calculator.util.ButtonUtil.enterNumberToast
import com.aatmik.calculator.util.ButtonUtil.invalidInputToast
import com.aatmik.calculator.util.ButtonUtil.vibratePhone
import com.aatmik.calculator.util.CalculationUtil
import com.aatmik.calculator.util.PrefUtil
import kotlin.math.sqrt


class ScientificCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentScientificCalculatorBinding

    companion object {
        var addedSC = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentScientificCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            addNumberValueToText(requireContext(), bt0SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt1SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt2SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt3SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt4SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt5SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt6SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt7SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt8SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), bt9SC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btSin, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btCos, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btTan, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btLog, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btLn, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btBracketOpenSC, tvPrimarySC, 1)
            addNumberValueToText(requireContext(), btBracketCloseSC, tvPrimarySC, 1)

            addOperatorValueToText(requireContext(), btAdditionSC, tvPrimarySC, "+", 1)
            addOperatorValueToText(requireContext(), btSubtractionSC, tvPrimarySC, "-", 1)
            addOperatorValueToText(requireContext(), btMultiplicationSC, tvPrimarySC, "*", 1)
            addOperatorValueToText(requireContext(), btDivisionSC, tvPrimarySC, "/", 1)

            btDotSC.setOnClickListener {
                vibratePhone(requireContext())
                if (!tvPrimarySC.text.contains(".")) tvPrimarySC.text =
                    tvPrimarySC.text.toString() + "."
            }

            btPi.setOnClickListener {
                vibratePhone(requireContext())

                tvPrimarySC.text = tvPrimarySC.text.toString() + "3.142"
                tvSecondarySC.text = btPi.text.toString()
            }

            btFactorial.setOnClickListener {
                vibratePhone(requireContext())

                try {
                    if (tvPrimarySC.text.isEmpty()) enterNumberToast(requireContext())
                    else {
                        val input = tvPrimarySC.text.toString()
                        var factorial = 1.0
                        for (i in 1..input.toLong()) {
                            factorial *= i
                        }
                        tvPrimarySC.text = CalculationUtil.trimResult(factorial.toString())
                        tvSecondarySC.text = "$input!"
                    }
                } catch (e: Exception) {
                    invalidInputToast(requireContext())
                }
            }

            btSquare.setOnClickListener {
                vibratePhone(requireContext())

                try {
                    if (tvPrimarySC.text.isEmpty()) enterNumberToast(requireContext())
                    else {
                        val input = tvPrimarySC.text.toString()
                        val result = (input.toFloat() * input.toFloat()).toString()
                        tvPrimarySC.text = CalculationUtil.trimResult(result)
                        tvSecondarySC.text = "$input²"
                    }
                } catch (e: Exception) {
                    invalidInputToast(requireContext())
                }
            }

            btInverted.setOnClickListener {
                vibratePhone(requireContext())

                try {
                    if (tvPrimarySC.text.isEmpty()) enterNumberToast(requireContext())
                    else {
                        val input = tvPrimarySC.text.toString()
                        val result = (1 / input.toFloat()).toString()
                        tvPrimarySC.text = CalculationUtil.trimResult(result)
                        tvSecondarySC.text = "1/$input"
                    }
                } catch (e: Exception) {
                    invalidInputToast(requireContext())
                }
            }

            btSqrt.setOnClickListener {
                vibratePhone(requireContext())

                try {
                    if (tvPrimarySC.text.isEmpty()) enterNumberToast(requireContext())
                    else {
                        val input = tvPrimarySC.text.toString()
                        val result = (sqrt(input.toFloat())).toString()
                        tvPrimarySC.text = CalculationUtil.trimResult(result)
                        tvSecondarySC.text = "√$input"
                    }
                } catch (e: Exception) {
                    invalidInputToast(requireContext())
                }
            }

            btACSC.setOnClickListener {
                vibratePhone(requireContext())

                tvPrimarySC.text = ""
                tvSecondarySC.text = ""
                addedSC = false
            }

            btDeleteSC.setOnClickListener {
                vibratePhone(requireContext())

                if (tvPrimarySC.text.contains("+") ||
                    tvPrimarySC.text.contains("-") ||
                    tvPrimarySC.text.contains("*") ||
                    tvPrimarySC.text.contains("/")
                ) addedSC = false

                if (tvPrimarySC.text.isNotEmpty()) tvPrimarySC.text =
                    tvPrimarySC.text.subSequence(0, tvPrimarySC.length() - 1)
            }

            btEqualSC.setOnClickListener {
                vibratePhone(requireContext())

                try {
                    if (tvPrimarySC.text.isNotEmpty()) {
                        val input = tvPrimarySC.text.toString()
                        val result = CalculationUtil.evaluate(input).toString()
                        tvPrimarySC.text = CalculationUtil.trimResult(result)
                        tvSecondarySC.text = input
                        addedSC = false
                    }
                } catch (e: Exception) {
                    invalidInputToast(requireContext())
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            tvPrimarySC.text = PrefUtil.getPrimaryTextSC(requireContext())
            tvSecondarySC.text = PrefUtil.getSecondaryTextSC(requireContext())
        }

    }

    override fun onStop() {
        super.onStop()
        binding.apply {
            PrefUtil.setPrimaryTextSC(requireContext(), tvPrimarySC.text.toString())
            PrefUtil.setSecondaryTextSC(requireContext(), tvSecondarySC.text.toString())
        }

    }
}