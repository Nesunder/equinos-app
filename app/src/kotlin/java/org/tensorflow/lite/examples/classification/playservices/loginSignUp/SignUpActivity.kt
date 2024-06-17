package org.tensorflow.lite.examples.classification.playservices.loginSignUp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivitySignUpBinding
import org.tensorflow.lite.examples.classification.playservices.settings.Network


class SignUpActivity : AppCompatActivity() {

    private lateinit var signUpBinding: ActivitySignUpBinding
    private var isPasswordVisible = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signUpBinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(signUpBinding.root)

        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        val loginActivityIntent = Intent(applicationContext, LoginActivity::class.java)
        signUpBinding.registerButton.setOnClickListener {
            lifecycleScope.launch {
                val validationState = validateInputs()
                if (validationState == ValidationState.VALID) {
                    startActivity(loginActivityIntent)
                    finish()
                } else {
                    if (validationState == ValidationState.NOT_REPEATED_PASSWORD) {
                        togglePasswordVisibility()
                    } else {
                        showValidationDialog(validationState)

                    }

                }
            }
        }
        signUpBinding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

    }

    private enum class ValidationState {
        VALID, NAME_EMPTY, EMAIL_EMPTY, PASSWORD_EMPTY, REPEAT_PASSWORD_EMPTY, ALL_EMPTY, NOT_REPEATED_PASSWORD, INVALID
    }

    private suspend fun validateInputs(): ValidationState {
        val username = signUpBinding.nombreEditText.text.toString().trim()
        val email = signUpBinding.emailEditText.text.toString().trim()
        val password = signUpBinding.passwordEditText.text.toString().trim()
        val repeatPassword = signUpBinding.repeatPasswordEditText.text.toString().trim()

        return when {
            password.isEmpty() && email.isEmpty() && username.isEmpty() && repeatPassword.isEmpty() -> {
                signUpBinding.nombreEditText.error = "Debe ingresar su nombre de usuario"
                signUpBinding.emailEditText.error = "Se debe ingresar el mail"
                signUpBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                signUpBinding.repeatPasswordEditText.error = "Por favor repita su contraseña"
                ValidationState.ALL_EMPTY
            }

            username.isEmpty() -> {
                signUpBinding.nombreEditText.error = "Debe ingresar su nombre de usuario"
                ValidationState.NAME_EMPTY
            }

            email.isEmpty() -> {
                signUpBinding.emailEditText.error = "Se debe ingresar el mail"
                ValidationState.EMAIL_EMPTY
            }

            password.isEmpty() -> {
                signUpBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                ValidationState.PASSWORD_EMPTY
            }

            repeatPassword.isEmpty() -> {
                signUpBinding.repeatPasswordEditText.error = "Por favor repita su contraseña"
                ValidationState.REPEAT_PASSWORD_EMPTY
            }

            password != repeatPassword -> {
                signUpBinding.repeatPasswordEditText.error = "Las contraseñas no coinciden"
                ValidationState.NOT_REPEATED_PASSWORD
            }

            else -> {
                performSignUp(username, email, password)
            }

        }
    }

    private suspend fun performSignUp(username: String, email: String, password: String): SignUpActivity.ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val json = JSONObject().apply {
                    put("username", username)
                    put("email", email)
                    put("password", password)
                }

                val requestBody =
                    json.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("${Network.URL}/api/auth/register") // TODO veterinario
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseCode = response.code
                val responseBody = response.body?.string()
                Log.d("LoginResponse", "Código de respuesta: $responseCode")
                Log.d("LoginResponse", "Cuerpo de la respuesta: $responseBody")

                if (response.isSuccessful) {
                    ValidationState.VALID
                } else {
                    ValidationState.INVALID
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Error durante la creacion de la cuenta: ${e.message}")
                ValidationState.INVALID
            }
        }
    }

    private fun showValidationDialog(validationState: ValidationState) {
        val message = when (validationState) {
            ValidationState.NAME_EMPTY -> "Debe ingresar su nombre"
            ValidationState.EMAIL_EMPTY -> "Por favor, ingrese su mail."
            ValidationState.PASSWORD_EMPTY -> "Por favor, ingrese su contraseña."
            ValidationState.ALL_EMPTY -> "Por favor, complete sus datos."
            ValidationState.REPEAT_PASSWORD_EMPTY -> "Por favor repita su contraseña"
            ValidationState.NOT_REPEATED_PASSWORD -> return
            ValidationState.VALID -> return
            ValidationState.INVALID -> "Error al crear la cuenta"
        }

        AlertDialog.Builder(this)
            .setTitle("Error de validación")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            signUpBinding.passwordEditText.transformationMethod =
                PasswordTransformationMethod.getInstance()
            signUpBinding.repeatPasswordEditText.transformationMethod =
                PasswordTransformationMethod.getInstance()
            signUpBinding.passwordToggle.setImageResource(R.drawable.ic_visibility_off)
        } else {
            signUpBinding.passwordEditText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            signUpBinding.repeatPasswordEditText.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            signUpBinding.passwordToggle.setImageResource(R.drawable.ic_visibility)
        }
        // Mueve el cursor al final del texto
        signUpBinding.passwordEditText.setSelection(signUpBinding.passwordEditText.length())
        signUpBinding.repeatPasswordEditText.setSelection(signUpBinding.repeatPasswordEditText.length())

        isPasswordVisible = !isPasswordVisible
    }
}