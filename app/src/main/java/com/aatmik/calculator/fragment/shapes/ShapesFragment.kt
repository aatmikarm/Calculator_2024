package com.aatmik.calculator.fragment.shapes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.ShapesAdapter
import com.aatmik.calculator.databinding.FragmentShapesBinding
import com.aatmik.calculator.fragment.CircleFragment
import com.aatmik.calculator.fragment.RectangleFragment
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
            openShapeDetailsFragment(shape)
        }

        binding.shapesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = shapesAdapter
        }
    }

    private fun openShapeDetailsFragment(shape: Shape) {
        val fragment = when (shape.name) {
            "Square" -> SquareFragment()
            "Circle" -> CircleFragment()
            "Rectangle" -> RectangleFragment()
            //"Triangle" -> TriangleFragment()
            //Pentagon" -> PentagonFragment()
            //"Hexagon" -> HexagonFragment()
            else -> throw IllegalArgumentException("Unknown shape: ${shape.name}")
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.calculatorFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG = "ShapesFragment"
    }
}