package com.aatmik.calculator.fragment


import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.adapter.CurrencyAdapter
import com.aatmik.calculator.databinding.CurrencySelectionDialogBinding
import com.aatmik.calculator.model.Currency

class CurrencySelectionDialog(
    private val availableCurrencies: List<Currency>,
    private val onCurrencySelected: (Currency) -> Unit,
) : DialogFragment() {

    private lateinit var binding: CurrencySelectionDialogBinding
    private lateinit var currencyAdapter: CurrencyAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = CurrencySelectionDialogBinding.inflate(layoutInflater)

        val dialog = Dialog(requireContext())
        dialog.setContentView(binding.root)

        setupRecyclerView()

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun setupRecyclerView() {
        currencyAdapter = CurrencyAdapter(availableCurrencies)
        binding.rvAvailableCurrencies.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = currencyAdapter
        }
        currencyAdapter.notifyDataSetChanged()
    }
}