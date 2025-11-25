package com.aatmik.calculator.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemCashFlowBinding
import com.aatmik.calculator.model.CashFlow

class CashFlowAdapter(
    private val cashFlows: MutableList<CashFlow>,
    private val onDelete: (Int) -> Unit,
    private val onUpdate: (Int, Double) -> Unit
) : RecyclerView.Adapter<CashFlowAdapter.CashFlowViewHolder>() {

    inner class CashFlowViewHolder(val binding: ItemCashFlowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashFlowViewHolder {
        val binding = ItemCashFlowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CashFlowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CashFlowViewHolder, position: Int) {
        val cashFlow = cashFlows[position]

        holder.binding.apply {
            tvYear.text = "Year ${cashFlow.year}"
            etCashFlow.setText(if (cashFlow.amount == 0.0) "" else cashFlow.amount.toString())

            // Remove previous text watcher
            etCashFlow.tag?.let { tag ->
                if (tag is TextWatcher) {
                    etCashFlow.removeTextChangedListener(tag)
                }
            }

            // Add new text watcher
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val amount = s.toString().toDoubleOrNull() ?: 0.0
                    onUpdate(holder.adapterPosition, amount)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }

            etCashFlow.tag = textWatcher
            etCashFlow.addTextChangedListener(textWatcher)

            btnDelete.setOnClickListener {
                onDelete(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount() = cashFlows.size
}