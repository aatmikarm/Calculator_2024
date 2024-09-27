package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R

class AgeHistoryAdapter(
    private var ageList: MutableList<String>,
    private val onItemClick: (String) -> Unit,
) :
    RecyclerView.Adapter<AgeHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val dobTextView: TextView = itemView.findViewById(R.id.dobTextView)
        val cakeIcon: ImageView = itemView.findViewById(R.id.cakeIcon)

        init {
            // Set click listener on the itemView
            itemView.setOnClickListener {
                onItemClick(ageList[adapterPosition]) // Call the click listener with the clicked item
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_age_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ageInfo =
            ageList[position].split(" - ") // Assuming your format is "Name - DOB: date - Age: age"
        if (ageInfo.size == 3) {
            holder.nameTextView.text = ageInfo[0] // User Name
            holder.dobTextView.text = ageInfo[1].replace("DOB: ", "") // Date of Birth
            // Here, you can set the user image using a placeholder or an actual image resource
            holder.userImageView.setImageResource(R.drawable.user) // Replace with your user image resource
            holder.cakeIcon.setImageResource(R.drawable.cake) // Set cake icon
        }
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
