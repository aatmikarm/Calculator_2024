package com.aatmik.calculator.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.CalculatorAdapter
import com.aatmik.calculator.databinding.ActivityMainBinding
import com.aatmik.calculator.model.Calculator
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adRequest: AdRequest
    private lateinit var interstitialAd1: InterstitialAd


    // recycler view
    lateinit var calculatorRV: RecyclerView
    lateinit var calculatorAdapter: CalculatorAdapter
    lateinit var calculatorList: ArrayList<Calculator>

    companion object {
        private const val GRID_COLUMN_COUNT = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this)
        adRequest = AdRequest.Builder().build()

        recyclerView()
        bannerAd()
        interstitialAd()
        search()

    }

    private fun search() {
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                filter(s.toString())
            }

            override fun afterTextChanged(p0: Editable?) {
                filter(p0.toString())
            }

        })
    }

    private fun recyclerView() {
        calculatorRV = binding.calculatorRV
        calculatorRV.layoutManager = GridLayoutManager(this, GRID_COLUMN_COUNT)
        calculatorList = createCalculatorList()
        calculatorAdapter = CalculatorAdapter(calculatorList) { calculator ->
            showToast("Clicked: ${calculator.name}")
            handleCalculatorSelection(calculator.name)
        }
        calculatorRV.adapter = calculatorAdapter
    }

    private fun createCalculatorList(): ArrayList<Calculator> {
        return arrayListOf(
            Calculator("Basic", R.drawable.abacus),
            Calculator("Convertor", R.drawable.convert),
            Calculator("Stopwatch", R.drawable.stopwatch),
            Calculator("Percentage", R.drawable.percentage),
            Calculator("Equation", R.drawable.equation_xy),
            Calculator("Shapes", R.drawable.shapes),
            Calculator("Bodies", R.drawable.bodies),
            Calculator("Length", R.drawable.length),
            Calculator("Speed", R.drawable.speed),
            Calculator("Temperature", R.drawable.temp),
            Calculator("Weight", R.drawable.weight),
            Calculator("Currency Converter", R.drawable.dollor),
            Calculator("Tip", R.drawable.tip),
            Calculator("Body Mass Index", R.drawable.bmi),
            Calculator("Age", R.drawable.cake),
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleCalculatorSelection(calculatorName: String) {
        val intent = when (calculatorName) {
            "Basic" -> Intent(this, CalculatorActivity::class.java)
            "Convertor" -> Intent(this, UnitConverterActivity::class.java)
            "Stopwatch" -> Intent(this, TimerActivity::class.java)
            else -> null
        }

        intent?.let { startActivity(it) }
    }

    private fun filter(text: String) {
        val filteredList: ArrayList<Calculator> = ArrayList()
        for (item in calculatorList) {
            if (item.name.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Calculator Not Found...", Toast.LENGTH_SHORT).show()
        } else {
            calculatorAdapter.filterList(filteredList)
        }
    }


    private fun showInterstitialAd() {
        interstitialAd1.show(this@MainActivity)
    }

    private fun interstitialAd() {
        InterstitialAd.load(this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd1 = interstitialAd
                }
            })

    }

    private fun bannerAd() {
        binding.bannerAd1.loadAd(adRequest)
    }

}