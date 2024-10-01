package com.aatmik.calculator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.databinding.ItemBodyBinding
import com.aatmik.calculator.model.Body


// Adapter class for the RecyclerView
class BodiesAdapter(
    private val bodies: List<Body>,
    private val onBodyClick: (Body) -> Unit,
) : RecyclerView.Adapter<BodiesAdapter.BodyViewHolder>() {

    inner class BodyViewHolder(private val binding: ItemBodyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(body: Body) {
            binding.bodyNameTv.text = body.name
            binding.bodyImageIv.setImageResource(body.imageResId)
            binding.root.setOnClickListener { onBodyClick(body) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BodyViewHolder {
        val binding = ItemBodyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BodyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BodyViewHolder, position: Int) {
        holder.bind(bodies[position])
    }

    override fun getItemCount() = bodies.size
}