package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.util.AnalyticsManager
import com.bumptech.glide.Glide

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

    /**
     * Filter list based on search query
     */
    fun filterList(filterList: ArrayList<Calculator>) {
        calculatorList = filterList
        notifyDataSetChanged()
    }

    /**
     * Update the list of calculators with a new list
     * This is used for category filtering and search
     */
    fun updateCalculatorList(updatedList: ArrayList<Calculator>) {
        val previousSize = calculatorList.size
        calculatorList.clear()
        calculatorList.addAll(updatedList)

        // Use more efficient notify methods
        if (previousSize == updatedList.size) {
            notifyItemRangeChanged(0, updatedList.size)
        } else {
            notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: CalculatorAdapter.CalculatorViewHolder, position: Int) {
        val calculator = calculatorList[position]
        holder.itemName.text = calculator.name

//        // Use direct setImageResource instead of Glide for drawables
//        // This is more reliable for resource drawables and prevents caching issues
//        holder.itemImage.setImageResource(calculator.image)
//
//        //holder.itemImage.setImageResource(calculator.image)
//        // now uses Glide with better image caching and loading
//        // it showing improper images when used
////        Glide.with(holder.itemView.context)
////            .load(calculator.image)
////            .into(holder.itemImage)

        // Use Glide with proper configuration to prevent wrong icons
        // Key fixes: dontAnimate() and dontTransform() prevent recycling issues
        Glide.with(holder.itemView.context)
            .load(calculator.image)
            .dontAnimate() // Prevent animation issues causing wrong icons
            .dontTransform() // Prevent transformation caching
            .override(200, 200) // Fixed size to prevent memory issues on low-end devices
            .centerInside() // Proper scaling
            .into(holder.itemImage)
    }

    override fun getItemCount(): Int {
        return calculatorList.size
    }

    /**
     * Get the current calculator list
     */
    fun getCalculatorList(): ArrayList<Calculator> {
        return calculatorList
    }

    /**
     * Check if the adapter is empty
     */
    fun isEmpty(): Boolean {
        return calculatorList.isEmpty()
    }

    inner class CalculatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemImage: ImageView = itemView.findViewById(R.id.itemImage)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < calculatorList.size) {
                    val calculator = calculatorList[position]

                    // Log calculator opened
                    AnalyticsManager.logCalculatorOpened(calculator.name)

                    onItemClick(calculator)

                }
            }
        }
    }
}