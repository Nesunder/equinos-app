package org.tensorflow.lite.examples.classification.playservices.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference

class Network {
    companion object {
        const val BASE_URL: String = "https://0eb9-201-235-161-12.ngrok-free.app"

        private var contextRef: WeakReference<Context>? = null

        fun initialize(context: Context) {
            contextRef = WeakReference(context.applicationContext)
        }

        fun setAccessToken(token: String?) {
            saveAccessTokenToPrefs(token)
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
