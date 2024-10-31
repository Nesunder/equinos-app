package com.equinos.appRepository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.equinos.ImageHelper
import com.equinos.settings.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UserRepository(private val context: Context) {

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    private val url = "/api/users"

    suspend fun uploadProfileImage(imageUri: Uri): Network.ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val image: ByteArray? = imageUri.let {
                    imageHelper.getByteArrayImage(
                        context, it
                    )
                }

                val boundary = "Boundary-" + System.currentTimeMillis()
                val lineEnd = "\r\n"

                val url = URL("${Network.BASE_URL}${url}/image")
                val connection = url.openConnection() as HttpURLConnection
                val token = Network.getAccessToken()

                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                val outputStream = DataOutputStream(connection.outputStream)

                image?.let {
                    outputStream.writeBytes("--$boundary$lineEnd")
                    outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"image\"; filename=\"imagen.jpg\"$lineEnd"
                    )
                    outputStream.writeBytes("Content-Type: image/jpeg$lineEnd")
                    outputStream.writeBytes(lineEnd)
                    outputStream.write(it)
                    outputStream.writeBytes(lineEnd)
                }

                outputStream.writeBytes("--$boundary--$lineEnd")
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
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
