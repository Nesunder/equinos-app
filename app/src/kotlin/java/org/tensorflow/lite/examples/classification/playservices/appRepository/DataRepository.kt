package org.tensorflow.lite.examples.classification.playservices.appRepository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseItem


object DataRepository {
    private val _data = MutableLiveData<List<HorseItem>>()
    val data: LiveData<List<HorseItem>> get() = _data

    init {
        _data.value = listOf()
    }

    fun updateData(newData: List<HorseItem>) {
        _data.value = newData
    }

    fun addItem(item: HorseItem) {
        val currentList = _data.value?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        _data.value = currentList
    }
}