package com.aatmik.calculator.fragment.bodies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.BodiesAdapter
import com.aatmik.calculator.databinding.FragmentBodiesBinding
import com.aatmik.calculator.fragment.ConeFragment
import com.aatmik.calculator.fragment.CubeFragment
import com.aatmik.calculator.fragment.EllipsoidFragment
import com.aatmik.calculator.fragment.FrustumFragment
import com.aatmik.calculator.fragment.PyramidFragment
import com.aatmik.calculator.fragment.RectangularPrismFragment
import com.aatmik.calculator.fragment.SphereFragment
import com.aatmik.calculator.fragment.TetrahedronFragment
import com.aatmik.calculator.model.Body

class BodiesFragment : Fragment() {

    private lateinit var binding: FragmentBodiesBinding
    private lateinit var bodiesAdapter: BodiesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBodiesBinding.inflate(inflater, container, false)
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
        val bodies = listOf(
            Body("Cube", R.drawable.cube),
            Body("Rectangular Prism", R.drawable.rectangular_prism),
            Body("Pyramid", R.drawable.pyramid),
            Body("Cone", R.drawable.cone),
            Body("Ellipsoid", R.drawable.ellipsoid),
            Body("Sphere", R.drawable.sphere),
            Body("Tetrahedron", R.drawable.tetrahedron),
            Body("Frustum", R.drawable.frustum),
        )

        bodiesAdapter = BodiesAdapter(bodies) { body ->
            openBodyDetailsFragment(body)
        }

        binding.bodiesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = bodiesAdapter
        }
    }

    private fun openBodyDetailsFragment(body: Body) {
        val fragment = when (body.name) {
            "Cube" -> CubeFragment()
            "Rectangular Prism" -> RectangularPrismFragment()
            "Pyramid" -> PyramidFragment()
            "Cone" -> ConeFragment()
            "Ellipsoid" -> EllipsoidFragment()
            "Sphere" -> SphereFragment()
            "Tetrahedron" -> TetrahedronFragment()
            "Frustum" -> FrustumFragment()
            else -> throw IllegalArgumentException("Unknown body: ${body.name}")
        }

        parentFragmentManager.beginTransaction().replace(R.id.calculatorFragmentContainer, fragment)
            .addToBackStack(null).commit()
    }

    companion object {
        private const val TAG = "BodiesFragment"
    }
}