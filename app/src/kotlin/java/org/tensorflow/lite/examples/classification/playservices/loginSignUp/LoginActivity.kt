package org.tensorflow.lite.examples.classification.playservices.loginSignUp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.tensorflow.lite.examples.classification.playservices.MainActivity
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityLoginBinding
import org.tensorflow.lite.examples.classification.playservices.settings.Network
import org.tensorflow.lite.examples.classification.playservices.settings.Network.Companion.setAccessToken

class LoginActivity : BaseActivity() {

    private lateinit var loginBinding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializar Network con el contexto de esta actividad
        Network.initialize(applicationContext)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginBinding.root)

        val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
        loginBinding.loginButton.setOnClickListener {
            lifecycleScope.launch {
                val validationState = validateInputs()
                if (validationState == ValidationState.VALID) {
                    val success = performLogin()
                    if (success) {
                        startActivity(mainActivityIntent)
                        finish()
                    } else {
                        showValidationDialog(this@LoginActivity, ValidationState.INVALID, getLoginMessages())
                    }
                } else {
                    showValidationDialog(this@LoginActivity, validationState, getLoginMessages())
                }
            }
        }
        loginBinding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun validateInputs(): ValidationState {
        val email = loginBinding.emailEditText.text.toString().trim()
        val password = loginBinding.passwordEditText.text.toString().trim()

        return when {
            email.isEmpty() && password.isEmpty() -> {
                loginBinding.emailEditText.error = "Se debe ingresar el mail"
                loginBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                ValidationState.ALL_EMPTY
            }

            email.isEmpty() -> {
                loginBinding.emailEditText.error = "Se debe ingresar el mail"
                ValidationState.EMAIL_EMPTY
            }

            password.isEmpty() -> {
                loginBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                ValidationState.PASSWORD_EMPTY
            }

            else -> ValidationState.VALID
        }
    }

    private suspend fun performLogin(): Boolean {
        val email = loginBinding.emailEditText.text.toString().trim()
        val password = loginBinding.passwordEditText.text.toString().trim()

        val requestBody = JSONObject().apply {
            put("identificacion", email)
            put("password", password)
        }

        return try {
            val response = performNetworkOperation("${Network.BASE_URL}/api/auth/login", requestBody)
            val jsonResponse = JSONObject(response)
            val accessToken = jsonResponse.getString("accessToken")
            setAccessToken(accessToken)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getLoginMessages(): Map<ValidationState, String> {
        return mapOf(
            ValidationState.EMAIL_EMPTY to "Por favor, ingrese su mail.",
            ValidationState.PASSWORD_EMPTY to "Por favor, ingrese su contraseña.",
            ValidationState.ALL_EMPTY to "Por favor, complete sus datos.",
            ValidationState.INVALID to "Error al iniciar sesión"
        )
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            loginBinding.passwordEditText.transformationMethod =
                PasswordTransformationMethod.getInstance()
            loginBinding.passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            loginBinding.passwordEditText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            loginBinding.passwordToggle.setImageResource(R.drawable.ic_visibility)
        }
        loginBinding.passwordEditText.setSelection(loginBinding.passwordEditText.length())
        isPasswordVisible = !isPasswordVisible
    }
}
