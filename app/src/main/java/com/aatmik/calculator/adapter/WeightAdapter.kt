package com.aatmik.calculator.adapter

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemUnitBinding
import com.aatmik.calculator.model.WeightUnit
import java.util.Collections

class WeightAdapter(
    private val units: ArrayList<WeightUnit>,
    private val onValueChanged: (Int, String) -> Unit,
) : RecyclerView.Adapter<WeightAdapter.UnitViewHolder>() {

    inner class UnitViewHolder(private val binding: ItemUnitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(unit: WeightUnit) {
            binding.unitSymbolTextView.text = unit.symbol
            binding.unitNameTextView.text = unit.name
            binding.unitConversionTextView.text = "${unit.conversionFactor} kg"
            binding.unitValueEditText.setText(unit.value)

            binding.unitValueEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    onValueChanged(adapterPosition, binding.unitValueEditText.text.toString())
                }
            }

            binding.unitValueEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (binding.unitValueEditText.hasFocus()) {
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

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(units, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val binding = ItemUnitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UnitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        holder.bind(units[position])
    }

    override fun getItemCount() = units.size

    fun updateUnit(position: Int, value: String) {
        units[position].value = value
        Handler(Looper.getMainLooper()).post {
            notifyItemChanged(position)
        }
    }

    fun getWeightUnitList(): ArrayList<WeightUnit> {
        return units
    }
}
