package com.example.myapplication.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R

class PageViewModel : ViewModel() {

    private var tabName = ""

    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: fuck"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }

    fun setName(name: String){
        tabName = name
    }
}