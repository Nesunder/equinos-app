package com.equinos.appRepository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.equinos.ImageHelper
import com.equinos.settings.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class UserRepository(private val context: Context) {

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    private val client = OkHttpClient()
    private val url = "/api/users"

    suspend fun uploadProfileImage(imageUri: Uri): Network.ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val image: ByteArray? = imageUri.let {
                    imageHelper.getByteArrayImage(
                        context, it
                    )
                }
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

                image?.let {
                    val imageMediaType = "image/jpeg".toMediaTypeOrNull()
                    val imagePart = it.toRequestBody(imageMediaType)
                    multipartBuilder.addFormDataPart("image", "imagen.jpg", imagePart)
                }
                val multipartBody = multipartBuilder.build()
                val token = Network.getAccessToken()

                val request =
                    Request.Builder().url("${Network.BASE_URL}${url}/image").post(multipartBody)
                        .addHeader("Authorization", "Bearer $token").build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Network.ValidationState.VALID
                } else {
                    Network.ValidationState.INVALID
                }
            } catch (e: IOException) {
                Log.d(TAG, e.stackTraceToString())
                e.printStackTrace()
                Network.ValidationState.INVALID
            }
        }
    }

    companion object {
        private val TAG = UserRepository::class.java.simpleName
    }
}