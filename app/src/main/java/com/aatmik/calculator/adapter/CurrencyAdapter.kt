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
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    init {
        // Enable stable IDs based on position
        setHasStableIds(true)
    }

    inner class CurrencyViewHolder(private val binding: CurrencyItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())
        private var runnable: Runnable? = null

        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (binding.etAmount.hasFocus()) {
                    runnable?.let { handler.removeCallbacks(it) }
                    runnable = Runnable {
                        onValueChanged(adapterPosition, s.toString())
                    }
                    handler.postDelayed(runnable!!, 300) // Debounce for smoother UI
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        fun bind(currency: Currency) {
            binding.tvCurrencyCode.text = currency.code
            binding.tvCurrencyName.text = currency.name
            binding.currencyRateTextView.text = currency.rate

            binding.etAmount.removeTextChangedListener(textWatcher)

            val newAmount = currency.inputAmount.toString()
            if (binding.etAmount.text.toString() != newAmount) {
                binding.etAmount.setText(newAmount)
            }

            binding.etAmount.addTextChangedListener(textWatcher)

            binding.etAmount.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    onValueChanged(adapterPosition, binding.etAmount.text.toString())
                }
            }
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

    // Use position as the ID since Currency does not have a unique ID
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun updateUnit(position: Int, value: String) {
        currencies[position].inputAmount = value.toDouble()
        Handler(Looper.getMainLooper()).post {
            notifyItemChanged(position)
        }
    }
}
