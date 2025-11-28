package com.aatmik.calculator.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.model.Category

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

        // Get the inner LinearLayout
        val contentLayout = holder.categoryCard.getChildAt(0)

        // Update appearance based on selection
        if (position == selectedPosition) {
            // Selected state - Border with theme color
            holder.categoryCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )

            // Create border programmatically
            val borderDrawable = GradientDrawable().apply {
                setColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
                setStroke(
                    6, // border width in pixels
                    ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)
                )
                cornerRadius = 12f * holder.itemView.context.resources.displayMetrics.density // 12dp in pixels
            }
            contentLayout?.background = borderDrawable

            holder.categoryName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)
            )
            holder.categoryDescription.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
            // Keep original icon colors even when selected
            holder.categoryIcon.clearColorFilter()
            holder.categoryCard.elevation = 8f
        } else {
            // Unselected state - White background with original icon colors
            holder.categoryCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )

            // Remove border
            contentLayout?.background = null

            holder.categoryName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.black)
            )
            holder.categoryDescription.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
            // Clear color filter to show original icon colors
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
        val categoryCard: CardView = itemView.findViewById(R.id.categoryCard)
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