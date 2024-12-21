package com.equinos.appRepository

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.equinos.horseCreation.HorseItem
import com.equinos.settings.Network
import java.net.HttpURLConnection
import java.net.URL

object HorseRepository {
    private val _data = MutableLiveData<List<HorseItem>>()
    val data: LiveData<List<HorseItem>> get() = _data

    init {
        _data.value = listOf()
    }

    fun updateData(newData: List<HorseItem>) {
        if (newData == _data.value) return
        _data.value = newData
    }

    fun addItem(item: HorseItem) {
        val currentList = _data.value?.toMutableList() ?: mutableListOf()
        currentList.add(item)
        _data.value = currentList
    }

    suspend fun loadInitialData(): List<HorseItem> {
        val response = performNetworkOperation("${Network.BASE_URL}/api/horses")
        val jsonResponse = JSONArray(response)
        val horseItemList = mutableListOf<HorseItem>()

        for (i in 0 until jsonResponse.length()) {
            val jsonObject: JSONObject = jsonResponse.getJSONObject(i)
            val nombre = jsonObject.getString("name")
            val id = jsonObject.getLong("id")
            val image = jsonObject.getString("image")
            val imageUri = "${Network.BASE_URL}/api/images/horses/compressed_$image"
            horseItemList.add(HorseItem(id, nombre, Uri.parse(imageUri)))
        }
        return horseItemList
    }

    private suspend fun performNetworkOperation(urlString: String): String {
        val token = Network.getAccessToken()
        return withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }
                } else {
                    throw Exception("HTTP error code: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
