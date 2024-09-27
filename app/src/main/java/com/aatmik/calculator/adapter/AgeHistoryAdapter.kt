package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R

class AgeHistoryAdapter(private var ageList: MutableList<String>) :
    RecyclerView.Adapter<AgeHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ageTextView: TextView = itemView.findViewById(R.id.ageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_age_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ageTextView.text = ageList[position]
    }

    override fun getItemCount() = ageList.size

    // This method is to remove an item from the list
    fun removeItem(position: Int) {
        ageList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItem(position: Int): String {
        return ageList[position]
    }

    fun updateList(newAgeList: List<String>) {
        ageList.clear() // Assuming 'ageList' is the dataset for your adapter
        ageList.addAll(newAgeList)
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }

}
