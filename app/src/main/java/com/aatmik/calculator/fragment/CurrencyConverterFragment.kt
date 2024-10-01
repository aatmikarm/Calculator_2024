package com.aatmik.calculator.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.adapter.CurrencyAdapter
import com.aatmik.calculator.databinding.FragmentCurrencyConvertorBinding
import com.aatmik.calculator.model.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class CurrencyConverterFragment : Fragment() {

    private var _binding: FragmentCurrencyConvertorBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CurrencyAdapter

    private val allCurrencies = mutableListOf<Currency>()
    private var initialCurrency: ArrayList<Currency> = arrayListOf(
        Currency("USD", "United States Dollar", "1"),
        Currency("EUR", "Euro", "1.11"),
        Currency("INR", "Indian Rupee", "0.012"),
        Currency("GBP", "British Pound", "1.33"),
        Currency("JPY", "Japanese Yen", "0.0069"),
        Currency("AUD", "Australian Dollar", "0.69"),
        Currency("CAD", "Canadian Dollar", "0.74"),
        Currency("CHF", "Swiss Franc", "1.18"),
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCurrencyConvertorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        fetchAllCurrencies()
        recyclerViewCode()
    }

    private fun recyclerViewCode() {
        adapter = CurrencyAdapter(initialCurrency) { position, newValue ->
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
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not handling swipe actions, so do nothing here
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.unitsRecyclerView)

    }

    private fun convertUnits(changedPosition: Int, newValue: String) {
        val changedUnit = initialCurrency[changedPosition]
        val newValueDouble = newValue.toDoubleOrNull() ?: return

        // Convert to base unit (meters)
        val valueInMeters = newValueDouble * changedUnit.rate!!.toDouble()

        // Update all other units
        initialCurrency.forEachIndexed { index, unit ->
            if (index != changedPosition) {
                val convertedValue = valueInMeters / unit.rate!!.toDouble()
                val formattedValue = "%.4f".format(convertedValue).trimEnd('0').trimEnd('.')
                unit.inputAmount = formattedValue.toDouble()
                adapter.updateUnit(index, formattedValue)
            }
        }
    }

    private fun fetchAllCurrencies() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currencyCodes = listOf(
                    Currency("USD", "United States Dollar"),
                    Currency("EUR", "Euro"),
                    Currency("INR", "Indian Rupee"),
                    Currency("GBP", "British Pound"),
                    Currency("JPY", "Japanese Yen"),
                    Currency("AUD", "Australian Dollar"),
                    Currency("CAD", "Canadian Dollar"),
                    Currency("CHF", "Swiss Franc"),
                )


                // Clear existing currency list if needed
                allCurrencies.clear()

                // Loop through each currency and get exchange rate (to USD for example)
                currencyCodes.forEach { currency ->
                    val rate = getCurrencyRate(currency.code, "USD") // Fetch rate for each currency
                    if (rate != null) {
                        val tempCurrency = currency.copy(rate = rate)
                        allCurrencies.add(tempCurrency)

                        // Update UI in the main thread as soon as a new currency is added
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "currency fetched then added: $tempCurrency")
                            Log.d(TAG, "fetchAllCurrencies: $allCurrencies")
                        }
                    }
                }

                // Log the total currencies loaded
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Currencies loaded: ${allCurrencies.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // Function to scrape exchange rate from Google
    private fun getCurrencyRate(fromCurrency: String, toCurrency: String): String? {
        val searchQuery = "$fromCurrency to $toCurrency"
        val url = "https://www.google.com/search?q=$searchQuery"
        return try {
            // Fetch the HTML document
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0") // User-Agent to avoid being blocked by Google
                .get()

            // Extract the exchange rate
            val rateElement = document.selectFirst("div[class=BNeawe iBp4i AP7Wnd]")
            val exchangeRate = rateElement?.text() ?: "N/A"
            exchangeRate
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "CurrencyConverterFragment"
    }

}


