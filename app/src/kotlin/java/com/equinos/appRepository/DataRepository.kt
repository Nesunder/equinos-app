package com.equinos.appRepository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.equinos.horseCreation.HorseItem
import com.equinos.settings.Network
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import android.util.Base64

object DataRepository {
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

    suspend fun loadInitialData(context: Context): List<HorseItem> {
        val response = performNetworkOperation("${Network.BASE_URL}/api/horses")
        val jsonResponse = JSONArray(response)
        val horseItemList = mutableListOf<HorseItem>()

        for (i in 0 until jsonResponse.length()) {
            val jsonObject: JSONObject = jsonResponse.getJSONObject(i)
            val nombre = jsonObject.getString("name")
            val id = jsonObject.getLong("id")
            val imageBytesBase64 = jsonObject.getString("compressedImage")
            val imageBytes = Base64.decode(imageBytesBase64, Base64.DEFAULT)
            val imageUri = saveImageToInternalStorage(context, imageBytes)
            horseItemList.add(HorseItem(id, nombre, imageUri))
        }

        return horseItemList
    }

    private fun saveImageToInternalStorage(context: Context, imageBytes: ByteArray): Uri {
        val fileName = "horse_image_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        try {
            val fos = FileOutputStream(file)
            fos.write(imageBytes)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.fromFile(file)
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
