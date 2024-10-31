package com.equinos.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.equinos.profile.Role
import com.equinos.profile.User
import java.lang.ref.WeakReference


class Network {
    enum class ValidationState {
        VALID, INVALID
    }
    companion object {
        const val BASE_URL: String = "https://21b7-181-168-26-49.ngrok-free.app"

        private var contextRef: WeakReference<Context>? = null
        private lateinit var sharedPreferences: SharedPreferences

        fun initialize(context: Context) {
            contextRef = WeakReference(context.applicationContext)
            sharedPreferences = getSharedPreferences(context)
        }

        fun setAccessToken(token: String?) {
            saveAccessTokenToPrefs(token)
        }

        fun setIdUsuario(idUsuario: String?) {
            saveIdUsuarioToPrefs(idUsuario)
        }

        fun getAccessToken(): String? {
            return sharedPreferences.getString("accessToken", "")
        }

        private fun clearAccessToken() {
            clearAccessTokenFromPrefs()
        }

        private fun saveAccessTokenToPrefs(token: String?) {
            with(sharedPreferences.edit()) {
                putString("accessToken", token)
                apply()
            }
        }

        private fun saveIdUsuarioToPrefs(idUsuario: String?) {
            with(sharedPreferences.edit()) {
                putString("idUsuario", idUsuario)
                apply()
            }
        }

        fun getIdUsuario(): Long {
            return sharedPreferences.getLong("userId", -1)
        }

        private fun clearAccessTokenFromPrefs() {
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

        fun saveUserData(user: User) {
            val editor = sharedPreferences.edit()
            editor.putLong("userId", user.userId)
            editor.putString("username", user.username)
            editor.putString("email", user.email)
            editor.putString("image", user.image.toString())
            editor.putString("role", user.role.toString())
            editor.putString("accessToken", user.accessToken)
            editor.apply()
        }

        fun getUserData(): User {
            val userId = sharedPreferences.getLong("userId", -1)
            val username = sharedPreferences.getString("username", null)
            val email = sharedPreferences.getString("email", null)
            val image = sharedPreferences.getString("image", null)
            val role = sharedPreferences.getString("role", null)
            val accessToken = sharedPreferences.getString("accessToken", null)
            return User(
                userId,
                username!!,
                email!!,
                Uri.parse(image),
                Role.valueOf(role!!),
                accessToken!!
            )
        }

        fun clearUserData() {
            clearAccessToken()
            with(sharedPreferences.edit()) {
                remove("userId")
                remove("username")
                remove("email")
                remove("image")
                remove("role")
                apply()
            }
        }

        fun saveStringProperty(property: String, value: String?) {
            with(sharedPreferences.edit()) {
                putString(property, value)
                apply()
            }
        }

        fun setNoConnectionMode() {
            val editor = sharedPreferences.edit()
            editor.putBoolean("noConnection", true)
            editor.apply()
        }

        fun getNoConnectionMode(): Boolean {
            return sharedPreferences.getBoolean("noConnection", false)
        }

        fun clearConnectionMode() {
            with(sharedPreferences.edit()) {
                remove("noConnection")
                apply()
            }
        }
    }
}
