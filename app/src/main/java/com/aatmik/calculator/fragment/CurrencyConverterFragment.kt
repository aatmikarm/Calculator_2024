package com.aatmik.calculator.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.adapter.CurrencyAdapter
import com.aatmik.calculator.databinding.FragmentCurrencyConvertorBinding
import com.aatmik.calculator.model.Currency
import com.aatmik.calculator.util.NetworkUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

class CurrencyConverterFragment : Fragment() {

    private var _binding: FragmentCurrencyConvertorBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CurrencyAdapter

    private val allCurrencies = mutableListOf<Currency>()


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
        adapter = CurrencyAdapter(allCurrencies) { position, newValue ->
            convertUnits(position, newValue)
        }

        binding.unitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.unitsRecyclerView.adapter = adapter

    }

    private fun convertUnits(changedPosition: Int, newValue: String) {
        val changedUnit = allCurrencies[changedPosition]
        val newValueDouble = newValue.toDoubleOrNull() ?: return

        // Convert to base unit (meters)
        val valueInMeters = newValueDouble * changedUnit.rate!!.toDouble()

        // Update all other units
        allCurrencies.forEachIndexed { index, unit ->
            if (index != changedPosition) {
                val currencyRateString = unit.rate!!
                val rateDoubleValue =
                    currencyRateString.split(" ")[0].toDouble() // Extracts the double value
                val convertedValue = valueInMeters / rateDoubleValue
                val formattedValue = "%.4f".format(convertedValue).trimEnd('0').trimEnd('.')
                unit.inputAmount = formattedValue.toDouble()
                adapter.updateUnit(index, formattedValue)
            }
        }
    }

    private fun fetchAllCurrencies() {

        // Check internet availability before proceeding
        if (!NetworkUtil.isNetworkAvailable(requireActivity())) {
            // Handle no internet case, maybe show a message to the user
            Toast.makeText(context, "No internet connection available.", Toast.LENGTH_SHORT).show()
            return
        }

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
                    Currency("CNY", "Chinese Yuan"),
                    Currency("SEK", "Swedish Krona")
                )

                // Clear existing currency list if needed
                allCurrencies.clear()
                binding.progressBar.visibility = View.VISIBLE

                // Parallel fetching of currency rates
                val deferredCurrencies = currencyCodes.map { currency ->
                    async {
                        val rate =
                            getCurrencyRate(currency.code, "USD") // Fetch rate for each currency
                        if (rate != null) {
                            currency.copy(rate = rate) // Return a new currency object with rate
                        } else {
                            null
                        }
                    }
                }

                // Wait for all the asynchronous tasks to complete
                val fetchedCurrencies = deferredCurrencies.awaitAll().filterNotNull()

                // Add fetched currencies to allCurrencies and sort by currency code
                allCurrencies.addAll(fetchedCurrencies)
                allCurrencies.sortBy { it.name } // Sort by currency code or any other attribute

                // Update the UI and log results on the main thread
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
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
            val exchangeRate = rateElement?.text() ?: "1.0"
            val value = exchangeRate.split(" ")[0].toDouble() // Extracts the double value


            Log.d(TAG, "getCurrencyRate: $fromCurrency to $toCurrency = $value ")
            value.toString()

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


