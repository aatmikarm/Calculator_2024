package com.aatmik.calculator.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AgeViewModel : ViewModel() {
    private val _ageList = MutableLiveData<List<String>>()
    val ageList: LiveData<List<String>> get() = _ageList

    fun updateAgeList(newAges: List<String>) {
        _ageList.value = newAges
    }
}
