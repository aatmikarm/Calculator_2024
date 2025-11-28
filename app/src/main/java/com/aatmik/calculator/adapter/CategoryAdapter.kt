package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.Category
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private var categoryList: ArrayList<Category>,
    private val onCategoryClick: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0 // Default "All" is selected

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.category_item,
            parent,
            false
        )
        return CategoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.categoryName.text = category.name
        holder.categoryDescription.text = category.description
        holder.categoryIcon.setImageResource(category.icon)

        // Get theme color dynamically (changes with theme selection)
        val typedValue = android.util.TypedValue()
        holder.itemView.context.theme.resolveAttribute(
            android.R.attr.colorPrimary,
            typedValue,
            true
        )
        val themeColor = typedValue.data

        // Update appearance based on selection
        if (position == selectedPosition) {
            // Selected state - Use dynamic theme color
            holder.categoryCard.strokeWidth = 6
            holder.categoryCard.strokeColor = themeColor  // Dynamic theme color!
            holder.categoryCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )

            holder.categoryName.setTextColor(themeColor)  // Dynamic theme color!
            holder.categoryDescription.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
            holder.categoryIcon.clearColorFilter()
            holder.categoryCard.elevation = 8f
        } else {
            // Unselected state - No border
            holder.categoryCard.strokeWidth = 0
            holder.categoryCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )

            holder.categoryName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
            holder.categoryDescription.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
            holder.categoryIcon.clearColorFilter()
            holder.categoryCard.elevation = 2f
        }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateSelection(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    fun getCurrentSelection(): Int {
        return selectedPosition
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryCard: MaterialCardView = itemView.findViewById(R.id.categoryCard)
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        val categoryDescription: TextView = itemView.findViewById(R.id.categoryDescription)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    updateSelection(position)
                    onCategoryClick(position)
                }
            }
        }
    }
}