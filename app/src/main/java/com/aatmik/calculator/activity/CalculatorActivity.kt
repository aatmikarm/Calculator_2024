package com.aatmik.calculator.activity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.ViewPagerAdapter
import com.aatmik.calculator.databinding.ActivityCalculatorBinding
import com.aatmik.calculator.fragment.BasicCalculatorFragment
import com.aatmik.calculator.fragment.ScientificCalculatorFragment

class CalculatorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalculatorBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.calculator)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        setUpTabs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setUpTabs() {
        binding.apply {
            val adapter = ViewPagerAdapter(supportFragmentManager)
            adapter.addFragment(BasicCalculatorFragment(), getString(R.string.basic_calculator))
            adapter.addFragment(
                ScientificCalculatorFragment(),
                getString(R.string.scientific_calculator)
            )
            viewPager.adapter = adapter
            tabLayout.setupWithViewPager(viewPager)
        }

    }
}