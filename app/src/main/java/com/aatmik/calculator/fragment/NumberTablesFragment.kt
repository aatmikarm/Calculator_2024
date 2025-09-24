package com.aatmik.calculator.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.MultiplicationTableAdapter
import com.aatmik.calculator.databinding.FragmentNumberTablesBinding
import com.aatmik.calculator.model.MultiplicationRow
import java.io.File
import java.io.FileOutputStream

class NumberTablesFragment : Fragment() {

    private lateinit var binding: FragmentNumberTablesBinding
    private lateinit var adapter: MultiplicationTableAdapter
    private var currentTableNumber = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNumberTablesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupUI()
        setupRecyclerView()
        hideKeyboardFunctionality(binding.main)
    }

    private fun setupUI() {
        binding.apply {
            // Back button click listener
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Share button click listener
            shareBt.setOnClickListener {
                if (currentTableNumber > 0) {
                    captureAndShareScreenshot()
                } else {
                    Toast.makeText(requireContext(), "Please enter a table number first", Toast.LENGTH_SHORT).show()
                }
            }

            // Clear button click listener
            clearBt.setOnClickListener {
                etTableNumber.text?.clear()
                clearTable()
            }

            // Setup text watcher for table number input
            etTableNumber.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val input = s.toString()
                    if (input.isNotEmpty()) {
                        try {
                            val tableNumber = input.toInt()
                            if (tableNumber > 0 && tableNumber <= 999) { // Allow reasonable range
                                currentTableNumber = tableNumber
                                generateMultiplicationTable(tableNumber)
                                clearTextIv.visibility = View.VISIBLE
                            } else if (tableNumber > 999) {
                                Toast.makeText(requireContext(), "Please enter a number between 1-999", Toast.LENGTH_SHORT).show()
                            } else if (tableNumber <= 0) {
                                Toast.makeText(requireContext(), "Please enter a positive number", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: NumberFormatException) {
                            clearTable()
                        }
                    } else {
                        clearTextIv.visibility = View.GONE
                        clearTable()
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            // Clear text button click listener
            clearTextIv.setOnClickListener {
                etTableNumber.text?.clear()
                clearTextIv.visibility = View.GONE
                hideKeyboard()
                etTableNumber.clearFocus()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MultiplicationTableAdapter(emptyList())
        val layoutManager = LinearLayoutManager(requireContext())
        binding.multiplicationTableRV.layoutManager = layoutManager
        binding.multiplicationTableRV.adapter = adapter
        binding.multiplicationTableRV.setHasFixedSize(false)
        binding.multiplicationTableRV.isNestedScrollingEnabled = false
    }

    private fun generateMultiplicationTable(tableNumber: Int) {
        val tableRows = mutableListOf<MultiplicationRow>()

        // Explicitly generate multiplication table from 1 to 12
        for (multiplier in 1..12) {
            val result = tableNumber * multiplier
            tableRows.add(MultiplicationRow(tableNumber, multiplier, result))
            Log.d(TAG, "Added row: $tableNumber × $multiplier = $result")
        }

        Log.d(TAG, "Total rows generated: ${tableRows.size}")

        // Update the adapter
        adapter.updateTable(tableRows)

        // Show the table card and hide the placeholder
        binding.apply {
            multiplicationTableCard.visibility = View.VISIBLE
            placeholderTextView.visibility = View.GONE

            // Update table title
            tableTitle.text = "Multiplication Table of $tableNumber"
        }
    }

    private fun clearTable() {
        currentTableNumber = 0
        adapter.updateTable(emptyList())

        binding.apply {
            multiplicationTableCard.visibility = View.GONE
            placeholderTextView.visibility = View.VISIBLE
        }
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.multiplicationTableCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "multiplication_table_$currentTableNumber.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get a content URI for the file using FileProvider
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Create a share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Multiplication Table of $currentTableNumber")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Multiplication Table"))
    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // Hide keyboard when clicked outside the keyboard or edit text functionality
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
        private const val TAG = "NumberTablesFragment"
    }
}