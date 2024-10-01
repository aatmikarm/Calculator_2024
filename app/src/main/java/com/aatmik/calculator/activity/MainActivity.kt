package com.aatmik.calculator.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.aatmik.calculator.adapter.CalculatorAdapter
import com.aatmik.calculator.databinding.ActivityMainBinding
import com.aatmik.calculator.databinding.BottomSheetLayoutBinding
import com.aatmik.calculator.model.Calculator
import com.aatmik.calculator.util.CalculatorUtils
import com.aatmik.calculator.util.NetworkUtil
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

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
        runAds()
        loadCalculatorOrder()
        setupRecyclerView()
        search()
        binding.menuIv.setOnClickListener {
            //showToast("menu clicked")
            showBottomSheet()
        }
        binding.calculatorCv.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            intent?.let { startActivity(it) }
        }
    }

    private fun runAds() {

        if (NetworkUtil.isNetworkAvailable(this)) {
            MobileAds.initialize(this)
            adRequest = AdRequest.Builder().build()
            bannerAd()
        } else {
            // Handle the case where there's no network available
            Log.d("NetworkCheck", "No internet connection available.")
        }

    }

    private fun bannerAd() {
        if (::adRequest.isInitialized) {
            binding.bannerAd1.loadAd(adRequest)
        }
    }

    private fun showInterstitialAd() {
        interstitialAd1.show(this@MainActivity)
    }

    private fun interstitialAd() {
        val prod = "ca-app-pub-5678552217308395/9237780167"
        val test = "ca-app-pub-3940256099942544/1033173712"

        if (::adRequest.isInitialized) {
            InterstitialAd.load(this, test, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd1 = interstitialAd
                    interstitialAd.show(this@MainActivity)
                }
            })
        }

    }


    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetLayoutBinding.inflate(layoutInflater)


        bottomSheetBinding.rateApp.setOnClickListener {
            rateApp()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnRemoveAds.setOnClickListener {
            removeAds()
            bottomSheetDialog.dismiss()
        }

        bottomSheetBinding.btnGetUpdate.setOnClickListener {
            getUpdate()
            bottomSheetDialog.dismiss()
        }
        bottomSheetBinding.btnTheme.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
    }

    private fun rateApp() {
        val appPackageName = "com.aatmik.calculator"
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName&showRating=true")
            )
        )
    }

    private fun removeAds() {
        // Implement your logic to remove ads or start premium subscription process
        Toast.makeText(this, "Removing ads / Starting premium subscription", Toast.LENGTH_SHORT)
            .show()
    }

    private fun getUpdate() {
        // Implement your logic to check for and get new updates
        Toast.makeText(this, "Checking for updates", Toast.LENGTH_SHORT).show()
    }

    private fun loadCalculatorOrder() {
        val sharedPreferences = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE)
        val savedOrder = sharedPreferences.getString("CalculatorOrder", null)

        if (savedOrder != null) {
            val orderedNames = savedOrder.split(",")
            val orderedList = arrayListOf<Calculator>()

            // Rebuild the calculator list based on saved order
            for (name in orderedNames) {
                val calculator = CalculatorUtils.calculatorList.find { it.name == name }
                calculator?.let {
                    orderedList.add(it)
                }
            }
            // Update the adapter with the ordered list
            //calculatorAdapter.updateCalculatorList(orderedList)
            calculatorList = orderedList
        } else {
            // Load default list if no saved order exists
            calculatorList = CalculatorUtils.calculatorList
            //calculatorAdapter.updateCalculatorList(calculatorList)
        }
    }


    private fun search() {
        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                filter(s.toString())
                // Show the clear (cross) button if there's text
                if (!s.isNullOrEmpty()) {
                    binding.clearTextIv.visibility = View.VISIBLE
                } else {
                    binding.clearTextIv.visibility = View.GONE
                }
                // Restore cursor visibility when the user starts typing again
                if (s.isNullOrEmpty().not()) {
                    binding.searchEt.isCursorVisible = true
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                filter(p0.toString())
            }
        })

        // Set up click listener for clear (cross) button
        binding.clearTextIv.setOnClickListener {
            // Clear the text
            binding.searchEt.text.clear()
            // Hide the clear button
            binding.clearTextIv.visibility = View.GONE
            // Hide the keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEt.windowToken, 0)
            // Hide the cursor when text is cleared
            binding.searchEt.isCursorVisible = false
        }

    }


    private fun setupRecyclerView() {
        calculatorRV = binding.calculatorRV
        calculatorRV.layoutManager = GridLayoutManager(this, GRID_COLUMN_COUNT)

        calculatorAdapter = CalculatorAdapter(calculatorList) { calculator ->
            //showToast("Clicked: ${calculator.name}")
            handleCalculatorSelection(calculator.name)
        }

        calculatorRV.adapter = calculatorAdapter

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
                calculatorAdapter.swapItems(fromPosition, toPosition)
                saveCalculatorOrder()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not handling swipe actions, so do nothing here
            }
        })

        itemTouchHelper.attachToRecyclerView(calculatorRV)
    }


    private fun handleCalculatorSelection(calculatorName: String) {
        val intent = when (calculatorName) {
            "Basic" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Convertor" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Stopwatch" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Percentage" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Age" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Length" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Weight" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Speed" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Tip" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Body Mass Index" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Shapes" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Equation" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Currency Converter" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            "Temperature" -> Intent(this, CalculatorActivity::class.java).apply {
                putExtra("calculatorName", calculatorName)
            }

            else -> null
        }

        intent?.let { startActivity(it) }
    }

    private fun filter(text: String) {
        val filteredList: ArrayList<Calculator> = ArrayList()
        for (item in calculatorList) {
            if (item.name.lowercase(Locale.getDefault())
                    .contains(text.lowercase(Locale.getDefault()))
            ) {
                filteredList.add(item)
            }
        }
        if (filteredList.isEmpty()) {
            //Toast.makeText(this, "Calculator Not Found...", Toast.LENGTH_SHORT).show()
        } else {
            calculatorAdapter.filterList(filteredList)
        }
    }

    private fun saveCalculatorOrder() {
        val sharedPreferences = getSharedPreferences("CalculatorPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get the current list of calculators from the adapter
        val updatedCalculatorList = calculatorAdapter.getCalculatorList()

        // Convert the list to a string and save it
        val order = updatedCalculatorList.joinToString(",") { it.name }
        editor.putString("CalculatorOrder", order)
        editor.apply() // Save changes
    }


}