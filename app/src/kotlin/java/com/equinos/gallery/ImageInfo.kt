package com.equinos.gallery

import android.content.Context
import com.google.gson.Gson
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties


data class ImageInfo(
    val title: String,
    val path: String,
    val size: Long,
    var prediction: String,
    var horseName: String,
    val userId: Long,
    val user: String
) {
    companion object {
        private const val PREFS_NAME = "PhotoPrefs"

        // Save a ImageInfo object
        fun save(context: Context, imageInfo: ImageInfo) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val gson = Gson()
            val json = gson.toJson(imageInfo)
            editor.putString(imageInfo.path, json) // Using photoPath as key
            editor.apply()
        }

        // Retrieve a ImageInfo object
        fun load(context: Context, photoPath: String): ImageInfo? {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            val json = sharedPreferences.getString(photoPath, null) ?: return null
            return gson.fromJson(json, ImageInfo::class.java)
        }

        // Update a specific field of PhotoInfo
        fun updateField(context: Context, photoPath: String, property: String, value: String) {
            val imageInfo = load(context, photoPath)
            imageInfo?.let {
                setProperty(it, property, value)
                save(context, it)
            }
        }

        fun getProperty(instance: Any, propertyName: String): Any? {
            val property = instance::class.memberProperties.find { it.name == propertyName }
            return property?.getter?.call(instance)
        }

        private fun setProperty(instance: Any, propertyName: String, value: Any?) {
            val property = instance::class.memberProperties.find { it.name == propertyName }
            if (property is KMutableProperty1<*, *>) {
                property.setter.call(instance, value)
            } else {
                throw IllegalArgumentException("Property $propertyName is not mutable or does not exist")
            }
        }

    }
}
