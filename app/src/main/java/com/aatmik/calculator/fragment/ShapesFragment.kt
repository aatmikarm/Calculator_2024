package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentShapesBinding
import com.aatmik.calculator.databinding.ItemShapeBinding
import com.aatmik.calculator.model.Shape

class ShapesFragment : Fragment() {

    private lateinit var binding: FragmentShapesBinding
    private lateinit var shapesAdapter: ShapesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentShapesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val shapes = listOf(
            Shape("Square", R.drawable.square),
            Shape("Circle", R.drawable.circle),
            Shape("Rectangle", R.drawable.rectangle),
            Shape("Triangle", R.drawable.triangle),
            Shape("Pentagon", R.drawable.pentagon),
            Shape("Hexagon", R.drawable.hexagon),
        )

        shapesAdapter = ShapesAdapter(shapes) { shape ->
            //openShapeDetailsFragment(shape)
        }

        binding.shapesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = shapesAdapter
        }
    }

    /*  private fun openShapeDetailsFragment(shape: Shape) {
          val shapeDetailsFragment = ShapeDetailsFragment.newInstance(shape.name)
          parentFragmentManager.beginTransaction()
              .replace(R.id.fragmentContainer, shapeDetailsFragment)
              .addToBackStack(null)
              .commit()
      }*/

    companion object {
        private const val TAG = "ShapesFragment"
    }
}

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