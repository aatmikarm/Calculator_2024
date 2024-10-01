package com.aatmik.calculator.adapter

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.CurrencyItemBinding
import com.aatmik.calculator.model.Currency

class CurrencyAdapter(
    private val currencies: MutableList<Currency>,
    private val onValueChanged: (Int, String) -> Unit,
) :
    RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    inner class CurrencyViewHolder(private val binding: CurrencyItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(currency: Currency) {
            binding.tvCurrencyCode.text = currency.code
            binding.tvCurrencyName.text = currency.name
            binding.currencyRateTextView.text = currency.rate

            binding.etAmount.setText(currency.inputAmount.toString())

            binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    onValueChanged(adapterPosition, binding.etAmount.text.toString())
                }
            }

            binding.etAmount.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (binding.etAmount.hasFocus()) {
                        onValueChanged(adapterPosition, s.toString())
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding =
            CurrencyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(currencies[position])
    }

    override fun getItemCount(): Int = currencies.size

    fun updateUnit(position: Int, value: String) {
        currencies[position].inputAmount = value.toDouble()
        Handler(Looper.getMainLooper()).post {
            notifyItemChanged(position)
        }
    }
}
