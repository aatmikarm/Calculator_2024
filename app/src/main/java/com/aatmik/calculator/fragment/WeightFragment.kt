package com.aatmik.calculator.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.adapter.WeightAdapter
import com.aatmik.calculator.databinding.FragmentWeightBinding
import com.aatmik.calculator.model.WeightUnit
import com.aatmik.calculator.util.CalculatorUtils

class WeightFragment : Fragment() {

    private lateinit var binding: FragmentWeightBinding
    private lateinit var adapter: WeightAdapter
    private lateinit var units: ArrayList<WeightUnit>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboardFunctionality(view)
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        loadCalculatorOrder()
        recyclerViewCode()
    }

    private fun recyclerViewCode() {
        adapter = WeightAdapter(units) { position, newValue ->
            convertUnits(position, newValue)
        }

        binding.weightRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.weightRecyclerView.adapter = adapter

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
                saveWeightOrder()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not handling swipe actions, so do nothing here
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.weightRecyclerView)
    }

    private fun convertUnits(changedPosition: Int, newValue: String) {
        val changedUnit = units[changedPosition]
        val newValueDouble = newValue.toDoubleOrNull() ?: return

        // Convert to base unit (kilograms)
        val valueInKilograms = newValueDouble * changedUnit.conversionFactor

        // Update all other units
        units.forEachIndexed { index, unit ->
            if (index != changedPosition) {
                val convertedValue = valueInKilograms / unit.conversionFactor
                val formattedValue = "%.4f".format(convertedValue).trimEnd('0').trimEnd('.')
                unit.value = formattedValue
                adapter.updateUnit(index, formattedValue)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun saveWeightOrder() {
        val sharedPreferences =
            requireContext().getSharedPreferences("CalculatorPrefs", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get the current list of units from the adapter
        val updatedWeightList = adapter.getWeightUnitList()

        // Convert the list to a string and save it
        val order = updatedWeightList.joinToString(",") { it.name }
        editor.putString("WeightUnitOrder", order)
        editor.apply() // Save changes
    }

    private fun loadCalculatorOrder() {
        val sharedPreferences =
            requireContext().getSharedPreferences("CalculatorPrefs", AppCompatActivity.MODE_PRIVATE)
        val savedOrder = sharedPreferences.getString("WeightUnitOrder", null)

        if (savedOrder != null) {
            val orderedNames = savedOrder.split(",")
            val orderedList = arrayListOf<WeightUnit>()

            // Rebuild the calculator list based on saved order
            for (name in orderedNames) {
                val calculator = CalculatorUtils.weightUnitList.find { it.name == name }
                calculator?.let {
                    orderedList.add(it)
                }
            }
            units = orderedList
        } else {
            // Load default list if no saved order exists
            units = CalculatorUtils.weightUnitList
        }
    }

    override fun onStop() {
        super.onStop()
        // Save the calculator order when the activity is stopped
        saveWeightOrder()
    }

    //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // hide keyboard when clicked outside the keyboard or edit text functionality
    private fun hideKeyboardFunctionality(view: View) {
        // Set up the touch listener for non-text box views
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // Get the currently focused view (e.g., EditText)
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus() // Clear focus to remove cursor from EditText
                }
            }
            false
        }
    }

    companion object {
        private const val TAG = "WeightFragment"
    }
}
