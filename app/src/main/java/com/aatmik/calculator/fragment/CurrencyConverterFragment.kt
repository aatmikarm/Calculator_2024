package com.aatmik.calculator.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.adapter.CurrencyAdapter
import com.aatmik.calculator.databinding.FragmentCurrencyConvertorBinding
import com.aatmik.calculator.interfaces.CurrencyApiService
import com.aatmik.calculator.model.Currency
import com.aatmik.calculator.model.ExchangeRateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class CurrencyConverterFragment : Fragment() {

    private var _binding: FragmentCurrencyConvertorBinding? = null
    private val binding get() = _binding!!

    private lateinit var currencyAdapter: CurrencyAdapter
    private val currencies = mutableListOf<Currency>()
    private val allCurrencies = mutableListOf<Currency>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.exchangerate-api.com/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(CurrencyApiService::class.java)

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

        binding.etAmount.addTextChangedListener { text ->
            updateConversions(text.toString())
        }

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
                    Currency("AUD", "Australian Dollar"),
                    Currency("CAD", "Canadian Dollar"),
                    Currency("CHF", "Swiss Franc"),
                    Currency("CNY", "Chinese Yuan"),
                    Currency("SEK", "Swedish Krona"),
                    Currency("NZD", "New Zealand Dollar"),
                    Currency("MXN", "Mexican Peso"),
                    Currency("SGD", "Singapore Dollar"),
                    Currency("HKD", "Hong Kong Dollar"),
                    Currency("NOK", "Norwegian Krone"),
                    Currency("KRW", "South Korean Won"),
                    Currency("TRY", "Turkish Lira"),
                    Currency("RUB", "Russian Ruble"),
                    Currency("BRL", "Brazilian Real"),
                    Currency("ZAR", "South African Rand"),
                    Currency("DKK", "Danish Krone"),
                    Currency("PLN", "Polish Zloty"),
                    Currency("THB", "Thai Baht"),
                    Currency("IDR", "Indonesian Rupiah"),
                    Currency("HUF", "Hungarian Forint"),
                    Currency("CZK", "Czech Koruna"),
                    Currency("ILS", "Israeli Shekel"),
                    Currency("CLP", "Chilean Peso"),
                    Currency("PHP", "Philippine Peso"),
                    Currency("AED", "United Arab Emirates Dirham"),
                    Currency("MYR", "Malaysian Ringgit"),
                    Currency("COP", "Colombian Peso"),
                    Currency("SAR", "Saudi Riyal"),
                    Currency("RON", "Romanian Leu"),
                    Currency("VND", "Vietnamese Dong"),
                    Currency("EGP", "Egyptian Pound"),
                    Currency("QAR", "Qatari Riyal"),
                    Currency("PKR", "Pakistani Rupee"),
                    Currency("KWD", "Kuwaiti Dinar"),
                    Currency("DZD", "Algerian Dinar"),
                    Currency("MAD", "Moroccan Dirham"),
                    Currency("JOD", "Jordanian Dinar"),
                    Currency("OMR", "Omani Rial"),
                    Currency("BHD", "Bahraini Dinar"),
                    Currency("IQD", "Iraqi Dinar"),
                    Currency("TND", "Tunisian Dinar"),
                    Currency("BDT", "Bangladeshi Taka"),
                    Currency("LKR", "Sri Lankan Rupee"),
                    Currency("XOF", "West African CFA Franc"),
                    Currency("XAF", "Central African CFA Franc"),
                    Currency("XCD", "East Caribbean Dollar"),
                    Currency("XPF", "CFP Franc"),
                    Currency("BND", "Brunei Dollar"),
                    Currency("MVR", "Maldivian Rufiyaa"),
                    Currency("SCR", "Seychellois Rupee"),
                    Currency("KES", "Kenyan Shilling"),
                    Currency("TZS", "Tanzanian Shilling"),
                    Currency("UGX", "Ugandan Shilling"),
                    Currency("GHS", "Ghanaian Cedi"),
                    Currency("NGN", "Nigerian Naira"),
                    Currency("ZMW", "Zambian Kwacha"),
                    Currency("ETB", "Ethiopian Birr"),
                    Currency("MUR", "Mauritian Rupee"),
                    Currency("NAD", "Namibian Dollar"),
                    Currency("LRD", "Liberian Dollar"),
                    Currency("SLL", "Sierra Leonean Leone"),
                    Currency("BWP", "Botswanan Pula"),
                    Currency("AOA", "Angolan Kwanza"),
                    Currency("MGA", "Malagasy Ariary"),
                    Currency("MWK", "Malawian Kwacha"),
                    Currency("CDF", "Congolese Franc"),
                    Currency("ZWL", "Zimbabwean Dollar"),
                    Currency("BZD", "Belize Dollar"),
                    Currency("GTQ", "Guatemalan Quetzal"),
                    Currency("HNL", "Honduran Lempira"),
                    Currency("PAB", "Panamanian Balboa"),
                    Currency("NIO", "Nicaraguan Córdoba"),
                    Currency("CRC", "Costa Rican Colón"),
                    Currency("UYU", "Uruguayan Peso"),
                    Currency("PYG", "Paraguayan Guarani"),
                    Currency("BOB", "Bolivian Boliviano"),
                    Currency("BAM", "Bosnia-Herzegovina Convertible Mark"),
                    Currency("MKD", "Macedonian Denar"),
                    Currency("ISK", "Icelandic Krona"),
                    Currency("GEL", "Georgian Lari"),
                    Currency("AZN", "Azerbaijani Manat"),
                    Currency("KZT", "Kazakhstani Tenge"),
                    Currency("AMD", "Armenian Dram"),
                    Currency("UZS", "Uzbekistani Som"),
                    Currency("TJS", "Tajikistani Somoni"),
                    Currency("AFN", "Afghan Afghani"),
                    Currency("YER", "Yemeni Rial"),
                    Currency("MMK", "Myanmar Kyat"),
                    Currency("LAK", "Lao Kip"),
                    Currency("KHR", "Cambodian Riel"),
                    Currency("MNT", "Mongolian Tugrik"),
                    Currency("PGK", "Papua New Guinean Kina"),
                    Currency("FJD", "Fijian Dollar"),
                    Currency("SBD", "Solomon Islands Dollar"),
                    Currency("VUV", "Vanuatu Vatu"),
                    Currency("TOP", "Tongan Paʻanga"),
                    Currency("WST", "Samoan Tala")
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

    private fun updateConversions(amount: String) {
        if (amount.isNotEmpty()) {
            val baseAmount = amount.toDoubleOrNull() ?: return
            apiService.getExchangeRates("USD").enqueue(object : Callback<ExchangeRateResponse> {
                override fun onResponse(
                    call: Call<ExchangeRateResponse>,
                    response: Response<ExchangeRateResponse>,
                ) {
                    if (response.isSuccessful) {
                        val rates = response.body()?.rates ?: return
                        currencies.forEach { currency ->
                            val rate = rates[currency.code] ?: 1.0
                        }
                        currencyAdapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<ExchangeRateResponse>, t: Throwable) {
                    // Handle error
                }
            })
        }
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
                updateConversions(binding.etAmount.text.toString())
            }
        }
        dialog.show(childFragmentManager, "CurrencySelectionDialog")
    }


    companion object {
        private const val TAG = "CurrencyConverterFragment"
    }

}


