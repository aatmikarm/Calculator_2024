package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator

class CalculatorAdapter(
    private var calculatorList: ArrayList<Calculator>,
    private val onItemClick: (Calculator) -> Unit,
) : RecyclerView.Adapter<CalculatorAdapter.CalculatorViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CalculatorAdapter.CalculatorViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.calculator_item,
            parent, false
        )
        return CalculatorViewHolder(itemView)
    }

    fun filterList(filterList: ArrayList<Calculator>) {
        calculatorList = filterList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CalculatorAdapter.CalculatorViewHolder, position: Int) {
        holder.itemName.text = calculatorList.get(position).name
        holder.itemImage.setImageResource(calculatorList.get(position).image)
    }

    override fun getItemCount(): Int {
        return calculatorList.size
    }

    inner class CalculatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)

        init {
            itemView.setOnClickListener {
                onItemClick(calculatorList[adapterPosition])
            }
        }
    }
}