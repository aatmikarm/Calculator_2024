package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemShapeBinding
import com.aatmik.calculator.model.Shape


// Adapter class for the RecyclerView
class ShapesAdapter(
    private val shapes: List<Shape>,
    private val onShapeClick: (Shape) -> Unit,
) : RecyclerView.Adapter<ShapesAdapter.ShapeViewHolder>() {

    inner class ShapeViewHolder(private val binding: ItemShapeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(shape: Shape) {
            binding.shapeNameTv.text = shape.name
            binding.shapeImageIv.setImageResource(shape.imageResId)
            binding.root.setOnClickListener { onShapeClick(shape) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShapeViewHolder {
        val binding = ItemShapeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShapeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShapeViewHolder, position: Int) {
        holder.bind(shapes[position])
    }

    override fun getItemCount() = shapes.size
}