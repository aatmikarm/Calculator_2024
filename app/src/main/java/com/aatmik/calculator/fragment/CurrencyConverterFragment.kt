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

    private lateinit var currencyAdapter: CurrencyAdapter
    private val currencies = mutableListOf<Currency>()
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

        binding.btnAdd.setOnClickListener {
            showCurrencySelectionDialog()
        }
    }

    private fun fetchAllCurrencies() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // List of currencies with their codes and names
                val currencyCodes = listOf(
                    Currency("USD", "United States Dollar"),
                    Currency("EUR", "Euro"),
                    Currency("INR", "Indian Rupee"),
                    Currency("GBP", "British Pound"),
                    Currency("JPY", "Japanese Yen"),
                ) // Added 100+ currency codes with names


                // Clear existing currency list if needed
                allCurrencies.clear()

                // Loop through each currency and get exchange rate (to INR for example)
                currencyCodes.forEach { currency ->
                    val rate = getCurrencyRate(currency.code, "INR") // Fetch rate for each currency
                    if (rate != null) {
                        allCurrencies.add(currency.copy(rate = rate))
                    }
                }

                // Now update the UI
                withContext(Dispatchers.Main) {
                    // Update UI with the new currency list (e.g., notify adapter)
                    Log.d(TAG, "fetchAllCurrencies: $allCurrencies")
                    Log.d(TAG, "Currencies loaded: ${allCurrencies.size}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Helper function to map currency codes to full currency names
    private fun getCurrencyName(code: String): String {
        return when (code) {
            "USD" -> "United States Dollar"
            "EUR" -> "Euro"
            "INR" -> "Indian Rupee"
            "GBP" -> "British Pound"
            "JPY" -> "Japanese Yen"
            "AUD" -> "Australian Dollar"
            "CAD" -> "Canadian Dollar"
            else -> "Unknown Currency"
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

    private fun setupRecyclerView() {
        currencyAdapter = CurrencyAdapter(currencies)
        binding.rvCurrencies.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = currencyAdapter
        }

        // Add initial currencies
        currencies.addAll(
            listOf(
                Currency("USD", "United States Dollar", "🇺🇸"),
                Currency("EUR", "Euro", "🇪🇺"),
                Currency("GBP", "British Pound", "🇬🇧"),
                Currency("JPY", "Japanese Yen", "🇯🇵")
            )
        )
        currencyAdapter.notifyDataSetChanged()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showCurrencySelectionDialog() {
        val dialog = CurrencySelectionDialog(allCurrencies) { selectedCurrency ->
            if (!currencies.contains(selectedCurrency)) {
                currencies.add(selectedCurrency)
                currencyAdapter.notifyItemInserted(currencies.size - 1)
            }
        }
        dialog.show(childFragmentManager, "CurrencySelectionDialog")
    }


    companion object {
        private const val TAG = "CurrencyConverterFragment"
    }

}


