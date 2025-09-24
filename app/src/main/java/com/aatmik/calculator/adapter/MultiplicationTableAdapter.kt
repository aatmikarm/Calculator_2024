package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.MultiplicationRow

class MultiplicationTableAdapter(
    private var tableRows: List<MultiplicationRow>
) : RecyclerView.Adapter<MultiplicationTableAdapter.MultiplicationViewHolder>() {

    class MultiplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTableNumber: TextView = itemView.findViewById(R.id.tvTableNumber)
        val tvMultiplier: TextView = itemView.findViewById(R.id.tvMultiplier)
        val tvResult: TextView = itemView.findViewById(R.id.tvResult)
        val tvEquation: TextView = itemView.findViewById(R.id.tvEquation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multiplication_row, parent, false)
        return MultiplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MultiplicationViewHolder, position: Int) {
        val row = tableRows[position]

        holder.tvTableNumber.text = row.tableNumber.toString()
        holder.tvMultiplier.text = row.multiplier.toString()
        holder.tvResult.text = row.result.toString()
        holder.tvEquation.text = row.getEquationString()
    }

    override fun getItemCount(): Int = tableRows.size

    fun updateTable(newTableRows: List<MultiplicationRow>) {
        this.tableRows = newTableRows
        notifyDataSetChanged()
    }
}