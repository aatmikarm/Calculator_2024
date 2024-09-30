package com.aatmik.calculator.network

import com.aatmik.calculator.model.CurrencyResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("latest/{baseCurrency}")
    fun getExchangeRates(@Path("baseCurrency") baseCurrency: String): Call<CurrencyResponse>
}
