package com.equinos.appRepository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.equinos.horseCreation.HorseItem

class MainViewModel : ViewModel() {
    val data: LiveData<List<HorseItem>> get() = DataRepository.data

    private val _newItemEvent = MutableLiveData<HorseItem>()
    val newItemEvent: LiveData<HorseItem> get() = _newItemEvent

    fun updateData(newItems: List<HorseItem>) {
        DataRepository.updateData(newItems)
    }

    fun addItem(item: HorseItem) {
        DataRepository.addItem(item)
    }

}