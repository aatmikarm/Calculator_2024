package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.adapter.LengthAdapter
import com.aatmik.calculator.databinding.FragmentLengthBinding
import com.aatmik.calculator.model.LengthUnit
import com.aatmik.calculator.util.CalculatorUtils

class LengthFragment : Fragment() {

    private lateinit var binding: FragmentLengthBinding
    private lateinit var adapter: LengthAdapter
    private lateinit var units: ArrayList<LengthUnit>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        loadCalculatorOrder()
        setupAddButton()
        recyclerViewCode()
    }

    private fun recyclerViewCode() {
        adapter = LengthAdapter(units) { position, newValue ->
            convertUnits(position, newValue)
        }

        binding.unitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.unitsRecyclerView.adapter = adapter


        // Set up drag-and-drop functionality
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0 // No swipe
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                // Swap items in the adapter
                adapter.swapItems(fromPosition, toPosition)
                saveLengthOrder()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not handling swipe actions, so do nothing here
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.unitsRecyclerView)

    }

    private fun convertUnits(changedPosition: Int, newValue: String) {
        val changedUnit = units[changedPosition]
        val newValueDouble = newValue.toDoubleOrNull() ?: return

        // Convert to base unit (meters)
        val valueInMeters = newValueDouble * changedUnit.conversionFactor

        // Update all other units
        units.forEachIndexed { index, unit ->
            if (index != changedPosition) {
                val convertedValue = valueInMeters / unit.conversionFactor
                val formattedValue = "%.6f".format(convertedValue).trimEnd('0').trimEnd('.')
                unit.value = formattedValue
                adapter.updateUnit(index, formattedValue)
            }
        }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            // Implement add unit functionality
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentLengthBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun saveLengthOrder() {
        val sharedPreferences =
            requireContext().getSharedPreferences("CalculatorPrefs", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get the current list of units from the adapter
        val updatedLengthList = adapter.getLengthUnitList()

        // Convert the list to a string and save it
        val order = updatedLengthList.joinToString(",") { it.name }
        editor.putString("LengthUnitOrder", order)
        editor.apply() // Save changes
    }

    private fun loadCalculatorOrder() {
        val sharedPreferences =
            requireContext().getSharedPreferences("CalculatorPrefs", AppCompatActivity.MODE_PRIVATE)
        val savedOrder = sharedPreferences.getString("LengthUnitOrder", null)

        if (savedOrder != null) {
            val orderedNames = savedOrder.split(",")
            val orderedList = arrayListOf<LengthUnit>()

            // Rebuild the calculator list based on saved order
            for (name in orderedNames) {
                val calculator = CalculatorUtils.lengthUnitList.find { it.name == name }
                calculator?.let {
                    orderedList.add(it)
                }
            }
            units = orderedList
        } else {
            // Load default list if no saved order exists
            units = CalculatorUtils.lengthUnitList
        }
    }

    override fun onStop() {
        super.onStop()
        // Save the calculator order when the activity is stopped
        saveLengthOrder()
    }

    companion object {
        private const val TAG = "LengthFragment"
    }
}