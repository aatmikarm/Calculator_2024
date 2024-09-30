package com.aatmik.calculator.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCurrencyConvertorBinding
import com.aatmik.calculator.model.CurrencyResponse
import com.aatmik.calculator.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CurrencyConverterFragment : Fragment() {

    private lateinit var binding: FragmentCurrencyConvertorBinding
    private lateinit var apiService: ApiService
    private var exchangeRates: Map<String, Double> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCurrencyConvertorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboardFunctionality(view)

        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        setupRetrofit()
        fetchExchangeRates()

        binding.convertButton.setOnClickListener {
            convertCurrency()
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.exchangerate-api.com/v4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun fetchExchangeRates() {
        if (isNetworkAvailable()) {
            apiService.getExchangeRates("USD").enqueue(object : Callback<CurrencyResponse> {
                override fun onResponse(
                    call: Call<CurrencyResponse>,
                    response: Response<CurrencyResponse>,
                ) {
                    if (response.isSuccessful) {
                        exchangeRates = response.body()?.rates ?: emptyMap()
                        updateUI()
                    } else {
                        showToast("Failed to fetch rates!")
                    }
                }

                override fun onFailure(call: Call<CurrencyResponse>, t: Throwable) {
                    showToast("Error: ${t.message}")
                }
            })
        } else {
            showToast("No internet connection!")
        }
    }

    private fun convertCurrency() {
        val amountString = binding.amountEditText.text.toString()
        val currencyFrom = binding.fromCurrencySpinner.selectedItem.toString()
        val currencyTo = binding.toCurrencySpinner.selectedItem.toString()

        val amount = amountString.toDoubleOrNull()
        if (amount != null) {
            val rateFrom = exchangeRates[currencyFrom] ?: 1.0
            val rateTo = exchangeRates[currencyTo] ?: 1.0
            val convertedAmount = amount * (rateTo / rateFrom)

            binding.resultTextView.text = "%.2f".format(convertedAmount)
        } else {
            showToast("Please enter a valid amount!")
        }
    }

    private fun updateUI() {
        // Update UI components such as spinners here if needed
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun hideKeyboardFunctionality(view: View) {
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus()
                }
            }
            false
        }
    }
}
