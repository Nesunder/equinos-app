package org.tensorflow.lite.examples.classification.playservices.loginSignUp

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

abstract class BaseActivity : AppCompatActivity() {

    enum class ValidationState {
        VALID,
        INVALID,
        EMAIL_EMPTY,
        PASSWORD_EMPTY,
        ALL_EMPTY,
        NAME_EMPTY,
        REPEAT_PASSWORD_EMPTY,
        NOT_REPEATED_PASSWORD
    }
    suspend fun performNetworkOperation(urlString: String, requestBody: JSONObject): String {
        return withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }

                val outputStream = connection.outputStream
                outputStream.use {
                    it.write(requestBody.toString().toByteArray(Charsets.UTF_8))
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

    fun showValidationDialog(context: Context, validationState: ValidationState, messages: Map<ValidationState, String>) {
        val message = messages[validationState] ?: return

        AlertDialog.Builder(context)
            .setTitle("Error de validación")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
