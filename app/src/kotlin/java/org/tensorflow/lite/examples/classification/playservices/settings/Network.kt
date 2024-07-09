package org.tensorflow.lite.examples.classification.playservices.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class Network {
    companion object {
        const val BASE_URL: String = "https://fc87-181-168-26-49.ngrok-free.app"

        private var contextRef: WeakReference<Context>? = null

        fun initialize(context: Context) {
            contextRef = WeakReference(context.applicationContext)
        }

        fun setAccessToken(token: String?) {
            saveAccessTokenToPrefs(token)
        }

        fun setIdUsuario(idUsuario: String?) {
            saveIdUsuarioToPrefs(idUsuario)
        }

        fun getAccessToken(): String? {
            val context = contextRef?.get() ?: return null
            val sharedPreferences = getSharedPreferences(context)
            return sharedPreferences.getString("accessToken", "")
        }

        fun clearAccessToken() {
            clearAccessTokenFromPrefs()
        }

        private fun saveAccessTokenToPrefs(token: String?) {
            val context = contextRef?.get() ?: return
            val sharedPreferences = getSharedPreferences(context)
            with(sharedPreferences.edit()) {
                putString("accessToken", token)
                apply()
            }
        }

        private fun saveIdUsuarioToPrefs(idUsuario: String?) {
            val context = contextRef?.get() ?: return
            val sharedPreferences = getSharedPreferences(context)
            with(sharedPreferences.edit()) {
                putString("idUsuario", idUsuario)
                apply()
            }
        }

        fun getIdUsuario(): String? {
            val context = contextRef?.get() ?: return null
            val sharedPreferences = getSharedPreferences(context)
            return sharedPreferences.getString("idUsuario", "")
        }

        private fun clearAccessTokenFromPrefs() {
            val context = contextRef?.get() ?: return
            val sharedPreferences = getSharedPreferences(context)
            with(sharedPreferences.edit()) {
                remove("accessToken")
                apply()
            }
        }

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                "user_prefs",
                AppCompatActivity.MODE_PRIVATE
            )
        }
    }
}
