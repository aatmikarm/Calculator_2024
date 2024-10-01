package com.aatmik.calculator.interfaces

import com.aatmik.calculator.model.ExchangeRateResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApiService {
    @GET("latest/USD")
    fun getExchangeRates(@Query("base") base: String): Call<ExchangeRateResponse>

    @GET("currencies.json")
    fun getCurrencies(): Call<Map<String, String>>
}