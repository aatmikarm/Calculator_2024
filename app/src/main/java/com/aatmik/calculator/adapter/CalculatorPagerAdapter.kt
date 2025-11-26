package com.aatmik.calculator.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aatmik.calculator.fragment.AllCalculatorsFragment
import com.aatmik.calculator.fragment.BasicCalculatorFragment

class CalculatorPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BasicCalculatorFragment()  // Left page - Basic Calculator
            1 -> AllCalculatorsFragment()   // Right page - All Calculators
            else -> BasicCalculatorFragment()
        }
    }
}