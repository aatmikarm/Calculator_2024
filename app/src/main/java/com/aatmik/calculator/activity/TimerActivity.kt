package com.aatmik.calculator.activity

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aatmik.calculator.R
import com.aatmik.calculator.adapter.ViewPagerAdapter
import com.aatmik.calculator.databinding.ActivityTimerBinding
import com.aatmik.calculator.fragment.CountdownTimerFragment
import com.aatmik.calculator.fragment.CounterFragment
import com.aatmik.calculator.fragment.StopwatchFragment

class TimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

       /* val actionBar = supportActionBar
        actionBar!!.title = getString(R.string.timer)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setBackgroundDrawable(ColorDrawable(Color.BLACK))*/

        setUpTabs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setUpTabs() {
        binding.apply {
            val adapter = ViewPagerAdapter(supportFragmentManager)
            adapter.addFragment(StopwatchFragment(), getString(R.string.stopwatch))
            adapter.addFragment(CountdownTimerFragment(), getString(R.string.countdown_timer))
            adapter.addFragment(CounterFragment(), getString(R.string.counter))
            viewPager.adapter = adapter
            tabLayout.setupWithViewPager(viewPager)
        }

    }
}
