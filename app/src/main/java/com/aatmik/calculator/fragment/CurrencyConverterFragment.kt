package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCurrencyConvertorBinding
import com.aatmik.calculator.model.Currency
import com.aatmik.calculator.util.NetworkUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class CurrencyConverterFragment : Fragment() {

    private var _binding: FragmentCurrencyConvertorBinding? = null
    private val binding get() = _binding!!

    private val allCurrencies = mutableListOf<Currency>()
    private var exchangeRates = mapOf<String, Double>()
    private var isDataLoaded = false

    // Cache configuration
    private val PREFS_NAME = "CurrencyRatesCache"
    private val KEY_RATES = "exchange_rates"
    private val KEY_TIMESTAMP = "last_update_timestamp"
    private val KEY_SOURCE = "data_source"
    private val CACHE_VALIDITY_HOURS = 24 // Refresh after 24 hours

    // Data sources - scrape from these websites (riding on their shoulders! 🔥)
    private val SOURCE_XRATES = "https://www.x-rates.com/table/?from=USD&amount=1"
    private val SOURCE_IBAN = "https://www.iban.com/exchange-rates"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d(TAG, "onCreateView: Fragment created")
        _binding = FragmentCurrencyConvertorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up views")
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupClickListeners()
        fetchExchangeRates()
    }

    private fun setupClickListeners() {
        binding.backIv.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.convertButton.setOnClickListener {
            if (isDataLoaded) {
                performConversion()
            } else {
                Toast.makeText(context, "Please wait, loading exchange rates...", Toast.LENGTH_SHORT).show()
            }
        }

        // Long press to force refresh
        binding.convertButton.setOnLongClickListener {
            if (NetworkUtil.isNetworkAvailable(requireActivity())) {
                Toast.makeText(context, "Refreshing exchange rates...", Toast.LENGTH_SHORT).show()
                clearCacheAndRefresh()
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
            true
        }

        binding.swapButton.setOnClickListener {
            swapCurrencies()
        }

        binding.amountEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isDataLoaded && !s.isNullOrEmpty()) {
                    performConversion()
                }
            }
        })

        // Long press to show cache info
        binding.exchangeRateInfo.setOnLongClickListener {
            showCacheInfo()
            true
        }
    }

    private fun clearCacheAndRefresh() {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "clearCacheAndRefresh: Cache cleared")
            isDataLoaded = false
            fetchExchangeRates()
        } catch (e: Exception) {
            Log.e(TAG, "clearCacheAndRefresh: Failed - ${e.message}", e)
        }
    }

    private fun showCacheInfo() {
        val cacheAge = getCacheAge()
        val source = getDataSource()
        val message = when {
            cacheAge == Long.MAX_VALUE -> "No cached data"
            cacheAge < 1 -> "Rates updated: < 1 hour ago\nSource: $source"
            cacheAge == 1L -> "Rates updated: 1 hour ago\nSource: $source"
            cacheAge < 24 -> "Rates updated: $cacheAge hours ago\nSource: $source"
            else -> {
                val days = cacheAge / 24
                "Rates updated: $days day${if (days > 1) "s" else ""} ago\nSource: $source\n(Long press Convert to refresh)"
            }
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun fetchExchangeRates() {
        Log.d(TAG, "fetchExchangeRates: START")

        if (!NetworkUtil.isNetworkAvailable(requireActivity())) {
            Log.e(TAG, "fetchExchangeRates: No network available")
            val cachedRates = loadCachedRates()
            if (cachedRates != null && cachedRates.isNotEmpty()) {
                Log.d(TAG, "fetchExchangeRates: Using cached rates (no internet)")
                exchangeRates = cachedRates
                buildCurrencyList()
                return
            }
            Toast.makeText(context, "No internet & no cached data", Toast.LENGTH_SHORT).show()
            return
        }

        // Check cache validity
        val cachedRates = loadCachedRates()
        val cacheAge = getCacheAge()

        if (cachedRates != null && cachedRates.isNotEmpty() && cacheAge < CACHE_VALIDITY_HOURS) {
            Log.d(TAG, "fetchExchangeRates: Using cached rates (age: $cacheAge hours)")
            exchangeRates = cachedRates
            buildCurrencyList()
            return
        }

        // Fetch fresh data
        Log.d(TAG, "fetchExchangeRates: Cache expired, fetching fresh data...")
        binding.progressBar.visibility = View.VISIBLE
        binding.convertButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var rates: Map<String, Double>? = null
                var source = "Unknown"

                // TRY SOURCE 1: X-Rates.com
                Log.d(TAG, "▶ Trying X-Rates.com...")
                try {
                    rates = scrapeXRates()
                    if (rates != null && rates.isNotEmpty()) {
                        source = "X-Rates.com"
                        Log.d(TAG, "✅ X-Rates.com SUCCESS - ${rates.size} currencies")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ X-Rates.com failed: ${e.message}")
                }

                // TRY SOURCE 2: IBAN.com (Fallback)
                if (rates == null || rates.isEmpty()) {
                    Log.d(TAG, "▶ Trying IBAN.com (fallback)...")
                    try {
                        rates = scrapeIBAN()
                        if (rates != null && rates.isNotEmpty()) {
                            source = "IBAN.com"
                            Log.d(TAG, "✅ IBAN.com SUCCESS - ${rates.size} currencies")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ IBAN.com failed: ${e.message}")
                    }
                }

                // TRY MERGING: Get maximum coverage
                if (rates != null && rates.isNotEmpty()) {
                    Log.d(TAG, "▶ Attempting to merge sources...")
                    val mergedRates = tryMergeSources(rates, source)
                    if (mergedRates.size > rates.size) {
                        val newCount = mergedRates.size - rates.size
                        Log.d(TAG, "✅ Merged +$newCount currencies from both sources")
                        rates = mergedRates
                        source = "X-Rates + IBAN (Merged)"
                    }
                }

                // SUCCESS
                if (rates != null && rates.isNotEmpty()) {
                    Log.d(TAG, "🎉 FINAL SUCCESS: ${rates.size} currencies from $source")
                    exchangeRates = rates
                    saveCachedRates(rates, source)

                    withContext(Dispatchers.Main) {
                        buildCurrencyList()
                        //Toast.makeText(context, "Rates from $source", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("All sources failed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ ALL SOURCES FAILED: ${e.message}", e)

                // Last resort: use old cache
                val fallbackRates = loadCachedRates()
                if (fallbackRates != null && fallbackRates.isNotEmpty()) {
                    Log.d(TAG, "⚠️ Using old cached data as last resort")
                    exchangeRates = fallbackRates
                    withContext(Dispatchers.Main) {
                        buildCurrencyList()
                        Toast.makeText(context, "Using old cached rates", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "All sources failed & no cache", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Scrape X-Rates.com
    private fun scrapeXRates(): Map<String, Double>? {
        try {
            val doc = Jsoup.connect(SOURCE_XRATES)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            val rates = mutableMapOf<String, Double>()
            rates["USD"] = 1.0

            // Find the rates table
            val rows = doc.select("table.ratesTable tbody tr, table.tablesorter tbody tr")

            rows.forEach { row ->
                try {
                    val cells = row.select("td")
                    if (cells.size >= 2) {
                        val currencyText = cells[0].text().trim()
                        val currencyCode = extractCurrencyCode(currencyText)
                        val rateText = cells[1].text().trim().replace(",", "")
                        val rate = rateText.toDoubleOrNull()

                        if (currencyCode != null && rate != null && rate > 0) {
                            rates[currencyCode] = rate
                        }
                    }
                } catch (e: Exception) {
                    // Skip bad rows
                }
            }

            return if (rates.size > 5) rates else null

        } catch (e: Exception) {
            Log.e(TAG, "scrapeXRates: ${e.message}")
            return null
        }
    }

    // Scrape IBAN.com
    private fun scrapeIBAN(): Map<String, Double>? {
        try {
            val doc = Jsoup.connect(SOURCE_IBAN)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            val rates = mutableMapOf<String, Double>()
            rates["USD"] = 1.0

            val rows = doc.select("table tr")

            rows.forEach { row ->
                try {
                    val cells = row.select("td")
                    if (cells.size >= 3) {
                        val currencyCode = cells[1].text().trim()
                        val rateText = cells[2].text().trim().replace(",", "")
                        val rate = rateText.toDoubleOrNull()

                        if (currencyCode.length == 3 && rate != null && rate > 0) {
                            rates[currencyCode] = rate
                        }
                    }
                } catch (e: Exception) {
                    // Skip bad rows
                }
            }

            return if (rates.size > 5) rates else null

        } catch (e: Exception) {
            Log.e(TAG, "scrapeIBAN: ${e.message}")
            return null
        }
    }

    // Merge data from both sources for maximum currency coverage
    private fun tryMergeSources(primaryRates: Map<String, Double>, primarySource: String): Map<String, Double> {
        val mergedRates = primaryRates.toMutableMap()

        try {
            // Get data from the OTHER source
            val secondaryRates = if (primarySource.contains("X-Rates")) {
                scrapeIBAN()
            } else {
                scrapeXRates()
            }

            secondaryRates?.forEach { (code, rate) ->
                if (!mergedRates.containsKey(code)) {
                    mergedRates[code] = rate
                    Log.d(TAG, "➕ Added $code from secondary source")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "tryMergeSources failed: ${e.message}")
        }

        return mergedRates
    }

    // Extract 3-letter currency code from text
    private fun extractCurrencyCode(text: String): String? {
        val currencyMap = mapOf(
            "Euro" to "EUR", "British Pound" to "GBP", "Indian Rupee" to "INR",
            "Japanese Yen" to "JPY", "Chinese Yuan" to "CNY", "Australian Dollar" to "AUD",
            "Canadian Dollar" to "CAD", "Swiss Franc" to "CHF", "Swedish Krona" to "SEK",
            "Norwegian Krone" to "NOK", "Danish Krone" to "DKK", "Singapore Dollar" to "SGD",
            "Hong Kong Dollar" to "HKD", "South Korean Won" to "KRW", "Mexican Peso" to "MXN",
            "Brazilian Real" to "BRL", "South African Rand" to "ZAR", "Russian Ruble" to "RUB",
            "Turkish Lira" to "TRY", "Polish Zloty" to "PLN", "Thai Baht" to "THB",
            "Indonesian Rupiah" to "IDR", "Malaysian Ringgit" to "MYR", "Philippine Peso" to "PHP",
            "New Zealand Dollar" to "NZD", "Czech Koruna" to "CZK", "Hungarian Forint" to "HUF",
            "Israeli Shekel" to "ILS", "Chilean Peso" to "CLP", "Colombian Peso" to "COP",
            "Argentine Peso" to "ARS", "Saudi Riyal" to "SAR", "UAE Dirham" to "AED",
            "Kuwaiti Dinar" to "KWD", "Pakistani Rupee" to "PKR", "Bangladeshi Taka" to "BDT",
            "Vietnamese Dong" to "VND", "Nigerian Naira" to "NGN", "Egyptian Pound" to "EGP"
        )

        // Try to find in map
        currencyMap.forEach { (name, code) ->
            if (text.contains(name, ignoreCase = true)) return code
        }

        // Extract 3-letter code directly
        val match = Regex("[A-Z]{3}").find(text)
        return match?.value
    }

    private fun buildCurrencyList() {
        val currencyNames = mapOf(
            "USD" to "United States Dollar", "EUR" to "Euro", "GBP" to "British Pound",
            "INR" to "Indian Rupee", "JPY" to "Japanese Yen", "CNY" to "Chinese Yuan",
            "AUD" to "Australian Dollar", "CAD" to "Canadian Dollar", "CHF" to "Swiss Franc",
            "SEK" to "Swedish Krona", "NOK" to "Norwegian Krone", "DKK" to "Danish Krone",
            "SGD" to "Singapore Dollar", "HKD" to "Hong Kong Dollar", "KRW" to "South Korean Won",
            "MXN" to "Mexican Peso", "BRL" to "Brazilian Real", "ZAR" to "South African Rand",
            "RUB" to "Russian Ruble", "TRY" to "Turkish Lira", "PLN" to "Polish Zloty",
            "THB" to "Thai Baht", "IDR" to "Indonesian Rupiah", "MYR" to "Malaysian Ringgit",
            "PHP" to "Philippine Peso", "NZD" to "New Zealand Dollar", "CZK" to "Czech Koruna",
            "HUF" to "Hungarian Forint", "ILS" to "Israeli Shekel", "CLP" to "Chilean Peso",
            "COP" to "Colombian Peso", "ARS" to "Argentine Peso", "SAR" to "Saudi Riyal",
            "AED" to "UAE Dirham", "KWD" to "Kuwaiti Dinar", "PKR" to "Pakistani Rupee",
            "BDT" to "Bangladeshi Taka", "VND" to "Vietnamese Dong", "NGN" to "Nigerian Naira",
            "EGP" to "Egyptian Pound", "QAR" to "Qatari Riyal", "RON" to "Romanian Leu",
            "TWD" to "Taiwan Dollar", "UAH" to "Ukrainian Hryvnia", "PEN" to "Peruvian Sol"
        )

        allCurrencies.clear()
        exchangeRates.forEach { (code, rate) ->
            val name = currencyNames[code] ?: code
            allCurrencies.add(Currency(code, name, rate.toString()))
        }

        allCurrencies.sortBy { it.code }

        CoroutineScope(Dispatchers.Main).launch {
            setupSpinners()
            isDataLoaded = true
            binding.progressBar.visibility = View.GONE
            binding.convertButton.isEnabled = true
            Log.d(TAG, "✅ UI ready with ${allCurrencies.size} currencies")
        }
    }

    private fun saveCachedRates(rates: Map<String, Double>, source: String) {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val jsonString = rates.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
            prefs.edit()
                .putString(KEY_RATES, "{$jsonString}")
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
                .putString(KEY_SOURCE, source)
                .apply()
            Log.d(TAG, "💾 Cached ${rates.size} rates from $source")
        } catch (e: Exception) {
            Log.e(TAG, "saveCachedRates failed: ${e.message}")
        }
    }

    private fun loadCachedRates(): Map<String, Double>? {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_RATES, null) ?: return null
            val rates = mutableMapOf<String, Double>()

            jsonString.trim().removeSurrounding("{", "}").split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val code = parts[0].replace("\"", "").trim()
                    val rate = parts[1].trim().toDoubleOrNull()
                    if (rate != null) rates[code] = rate
                }
            }

            return rates
        } catch (e: Exception) {
            return null
        }
    }

    private fun getCacheAge(): Long {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0)
        return if (timestamp == 0L) Long.MAX_VALUE else (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60)
    }

    private fun getDataSource(): String {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getString(KEY_SOURCE, "Unknown") ?: "Unknown"
    }

    private fun setupSpinners() {
        Log.d(TAG, "setupSpinners: Setting up currency spinners with flags")

        // Create display strings with flags for spinner
        val currencyDisplayList = allCurrencies.map {
            "${getFlagEmoji(it.code)} ${it.code} - ${it.name}"
        }

        // Create adapter
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencyDisplayList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set adapters
        binding.fromCurrencySpinner.adapter = adapter
        binding.toCurrencySpinner.adapter = adapter

        // Set default selections (USD to INR for Indian users)
        val usdPos = allCurrencies.indexOfFirst { it.code == "USD" }
        val inrPos = allCurrencies.indexOfFirst { it.code == "INR" }
        if (usdPos >= 0) binding.fromCurrencySpinner.setSelection(usdPos)
        if (inrPos >= 0) binding.toCurrencySpinner.setSelection(inrPos)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateExchangeRateInfo()
                if (!binding.amountEditText.text.isNullOrEmpty()) performConversion()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.fromCurrencySpinner.onItemSelectedListener = listener
        binding.toCurrencySpinner.onItemSelectedListener = listener
        updateExchangeRateInfo()

        // Update popular currency cards
        updatePopularRatesCards()

        Log.d(TAG, "setupSpinners: Spinners setup complete with flags")
    }

    // Get flag emoji for currency code
    private fun getFlagEmoji(currencyCode: String): String {
        return when (currencyCode) {
            // Americas
            "USD" -> "🇺🇸"
            "CAD" -> "🇨🇦"
            "MXN" -> "🇲🇽"
            "BRL" -> "🇧🇷"
            "ARS" -> "🇦🇷"
            "CLP" -> "🇨🇱"
            "COP" -> "🇨🇴"
            "PEN" -> "🇵🇪"
            "CRC" -> "🇨🇷"
            "UYU" -> "🇺🇾"
            "BOB" -> "🇧🇴"
            "GTQ" -> "🇬🇹"
            "DOP" -> "🇩🇴"
            "JMD" -> "🇯🇲"
            "TTD" -> "🇹🇹"
            "BSD" -> "🇧🇸"
            "BBD" -> "🇧🇧"

            // Europe
            "EUR" -> "🇪🇺"
            "GBP" -> "🇬🇧"
            "CHF" -> "🇨🇭"
            "NOK" -> "🇳🇴"
            "SEK" -> "🇸🇪"
            "DKK" -> "🇩🇰"
            "PLN" -> "🇵🇱"
            "CZK" -> "🇨🇿"
            "HUF" -> "🇭🇺"
            "RON" -> "🇷🇴"
            "BGN" -> "🇧🇬"
            "HRK" -> "🇭🇷"
            "RUB" -> "🇷🇺"
            "UAH" -> "🇺🇦"
            "TRY" -> "🇹🇷"
            "ISK" -> "🇮🇸"
            "RSD" -> "🇷🇸"
            "MDL" -> "🇲🇩"
            "MKD" -> "🇲🇰"
            "ALL" -> "🇦🇱"
            "BAM" -> "🇧🇦"

            // Asia
            "INR" -> "🇮🇳"
            "CNY" -> "🇨🇳"
            "JPY" -> "🇯🇵"
            "KRW" -> "🇰🇷"
            "SGD" -> "🇸🇬"
            "HKD" -> "🇭🇰"
            "TWD" -> "🇹🇼"
            "THB" -> "🇹🇭"
            "MYR" -> "🇲🇾"
            "IDR" -> "🇮🇩"
            "PHP" -> "🇵🇭"
            "VND" -> "🇻🇳"
            "PKR" -> "🇵🇰"
            "BDT" -> "🇧🇩"
            "LKR" -> "🇱🇰"
            "NPR" -> "🇳🇵"
            "MMK" -> "🇲🇲"
            "KHR" -> "🇰🇭"
            "LAK" -> "🇱🇦"
            "BND" -> "🇧🇳"
            "KZT" -> "🇰🇿"
            "UZS" -> "🇺🇿"
            "MNT" -> "🇲🇳"
            "AZN" -> "🇦🇿"
            "GEL" -> "🇬🇪"
            "AMD" -> "🇦🇲"
            "KGS" -> "🇰🇬"
            "TJS" -> "🇹🇯"
            "TMT" -> "🇹🇲"
            "AFN" -> "🇦🇫"
            "MVR" -> "🇲🇻"
            "BTN" -> "🇧🇹"

            // Middle East
            "AED" -> "🇦🇪"
            "SAR" -> "🇸🇦"
            "QAR" -> "🇶🇦"
            "KWD" -> "🇰🇼"
            "BHD" -> "🇧🇭"
            "OMR" -> "🇴🇲"
            "JOD" -> "🇯🇴"
            "ILS" -> "🇮🇱"
            "LBP" -> "🇱🇧"
            "SYP" -> "🇸🇾"
            "IQD" -> "🇮🇶"
            "IRR" -> "🇮🇷"
            "YER" -> "🇾🇪"

            // Africa
            "ZAR" -> "🇿🇦"
            "EGP" -> "🇪🇬"
            "NGN" -> "🇳🇬"
            "KES" -> "🇰🇪"
            "GHS" -> "🇬🇭"
            "TZS" -> "🇹🇿"
            "UGX" -> "🇺🇬"
            "MAD" -> "🇲🇦"
            "TND" -> "🇹🇳"
            "DZD" -> "🇩🇿"
            "XOF" -> "🇸🇳" // West African (Senegal flag)
            "XAF" -> "🇨🇲" // Central African (Cameroon flag)
            "ETB" -> "🇪🇹"
            "MUR" -> "🇲🇺"
            "BWP" -> "🇧🇼"
            "NAD" -> "🇳🇦"
            "ZMW" -> "🇿🇲"
            "AOA" -> "🇦🇴"
            "MZN" -> "🇲🇿"
            "RWF" -> "🇷🇼"
            "SOS" -> "🇸🇴"
            "SDG" -> "🇸🇩"
            "LYD" -> "🇱🇾"
            "MWK" -> "🇲🇼"
            "SCR" -> "🇸🇨"
            "SZL" -> "🇸🇿"
            "LSL" -> "🇱🇸"
            "GMD" -> "🇬🇲"
            "GNF" -> "🇬🇳"
            "SLL" -> "🇸🇱"
            "LRD" -> "🇱🇷"

            // Oceania
            "AUD" -> "🇦🇺"
            "NZD" -> "🇳🇿"
            "FJD" -> "🇫🇯"
            "PGK" -> "🇵🇬"
            "TOP" -> "🇹🇴"
            "WST" -> "🇼🇸"
            "SBD" -> "🇸🇧"
            "VUV" -> "🇻🇺"

            // Precious Metals / Others
            "XAU" -> "🥇" // Gold
            "XAG" -> "🥈" // Silver
            "XPT" -> "⚪" // Platinum
            "XPD" -> "⚫" // Palladium

            else -> "🏳️" // Default flag
        }
    }

    private fun updatePopularRatesCards() {
        try {
            // Update all 20 popular currency cards
            exchangeRates["EUR"]?.let { binding.eurRate.text = String.format("%.2f", it) }
            exchangeRates["INR"]?.let { binding.inrRate.text = String.format("%.2f", it) }
            exchangeRates["GBP"]?.let { binding.gbpRate.text = String.format("%.2f", it) }
            exchangeRates["JPY"]?.let { binding.jpyRate.text = String.format("%.2f", it) }
            exchangeRates["AUD"]?.let { binding.audRate.text = String.format("%.2f", it) }
            exchangeRates["CAD"]?.let { binding.cadRate.text = String.format("%.2f", it) }
            exchangeRates["CHF"]?.let { binding.chfRate.text = String.format("%.2f", it) }
            exchangeRates["CNY"]?.let { binding.cnyRate.text = String.format("%.2f", it) }
            exchangeRates["SEK"]?.let { binding.sekRate.text = String.format("%.2f", it) }
            exchangeRates["NZD"]?.let { binding.nzdRate.text = String.format("%.2f", it) }
            exchangeRates["SGD"]?.let { binding.sgdRate.text = String.format("%.2f", it) }
            exchangeRates["HKD"]?.let { binding.hkdRate.text = String.format("%.2f", it) }
            exchangeRates["KRW"]?.let { binding.krwRate.text = String.format("%.0f", it) }
            exchangeRates["NOK"]?.let { binding.nokRate.text = String.format("%.2f", it) }
            exchangeRates["MXN"]?.let { binding.mxnRate.text = String.format("%.2f", it) }
            exchangeRates["ZAR"]?.let { binding.zarRate.text = String.format("%.2f", it) }
            exchangeRates["THB"]?.let { binding.thbRate.text = String.format("%.2f", it) }
            exchangeRates["BRL"]?.let { binding.brlRate.text = String.format("%.2f", it) }
            exchangeRates["RUB"]?.let { binding.rubRate.text = String.format("%.2f", it) }
            exchangeRates["TRY"]?.let { binding.tryRate.text = String.format("%.2f", it) }

            Log.d(TAG, "✅ All 20 popular rate cards updated")
        } catch (e: Exception) {
            Log.e(TAG, "updatePopularRatesCards failed: ${e.message}")
        }
    }

    private fun performConversion() {
        val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: return
        if (amount <= 0) return

        val fromPos = binding.fromCurrencySpinner.selectedItemPosition
        val toPos = binding.toCurrencySpinner.selectedItemPosition
        if (fromPos < 0 || toPos < 0) return

        val fromRate = allCurrencies[fromPos].rate?.toDoubleOrNull() ?: return
        val toRate = allCurrencies[toPos].rate?.toDoubleOrNull() ?: return

        val result = (amount / fromRate) * toRate
        val formatted = when {
            result >= 1000 -> String.format("%,.2f", result)
            result >= 1 -> String.format("%.2f", result)
            else -> String.format("%.4f", result)
        }

        binding.resultTextView.text = formatted
    }

    private fun updateExchangeRateInfo() {
        val fromPos = binding.fromCurrencySpinner.selectedItemPosition
        val toPos = binding.toCurrencySpinner.selectedItemPosition
        if (fromPos < 0 || toPos < 0) return

        val fromCurrency = allCurrencies[fromPos]
        val toCurrency = allCurrencies[toPos]
        val fromRate = fromCurrency.rate?.toDoubleOrNull() ?: return
        val toRate = toCurrency.rate?.toDoubleOrNull() ?: return
        val rate = toRate / fromRate

        binding.exchangeRateInfo.text = "1 ${fromCurrency.code} = ${String.format("%.4f", rate)} ${toCurrency.code}"
    }

    private fun swapCurrencies() {
        val from = binding.fromCurrencySpinner.selectedItemPosition
        val to = binding.toCurrencySpinner.selectedItemPosition
        binding.fromCurrencySpinner.setSelection(to)
        binding.toCurrencySpinner.setSelection(from)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "CurrencyConverter"
    }
}