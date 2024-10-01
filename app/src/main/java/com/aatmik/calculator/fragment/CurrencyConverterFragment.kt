package com.aatmik.calculator.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        setupRecyclerView()
        fetchAllCurrencies()
    }

    private fun setupRecyclerView() {
        binding.rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun fetchAllCurrencies() {
        // Initialize the adapter and set it to the RecyclerView
        val adapter = CurrencyAdapter(allCurrencies)
        binding.rvCurrencies.adapter = adapter

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
                )


                // Clear existing currency list if needed
                allCurrencies.clear()

                // Loop through each currency and get exchange rate (to USD for example)
                currencyCodes.forEach { currency ->
                    val rate = getCurrencyRate(currency.code, "INR") // Fetch rate for each currency
                    if (rate != null) {
                        val tempCurrency = currency.copy(rate = rate)
                        allCurrencies.add(tempCurrency)

                        // Update UI in the main thread as soon as a new currency is added
                        withContext(Dispatchers.Main) {
                            adapter.notifyItemInserted(allCurrencies.size - 1) // Notify adapter of the new item
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


