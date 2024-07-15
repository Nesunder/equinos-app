package com.equinos.loginSignUp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.equinos.R
import com.equinos.databinding.ActivitySignUpBinding
import com.equinos.settings.Network

class SignUpActivity : BaseActivity() {

    private lateinit var signUpBinding: ActivitySignUpBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        signUpBinding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(signUpBinding.root)

        val loginActivityIntent = Intent(applicationContext, LoginActivity::class.java)
        signUpBinding.registerButton.setOnClickListener {
            lifecycleScope.launch {
                val validationState = validateInputs()
                if (validationState == ValidationState.VALID) {
                    val success = performSignUp()
                    if (success) {
                        startActivity(loginActivityIntent)
                        finish()
                    } else {
                        showValidationDialog(this@SignUpActivity, ValidationState.INVALID, getSignUpMessages())
                    }
                } else {
                    if (validationState == ValidationState.NOT_REPEATED_PASSWORD) {
                        togglePasswordVisibility()
                    } else {
                        showValidationDialog(this@SignUpActivity, validationState, getSignUpMessages())
                    }
                }
            }
        }
        signUpBinding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun validateInputs(): ValidationState {
        val username = signUpBinding.nombreEditText.text.toString().trim()
        val email = signUpBinding.emailEditText.text.toString().trim()
        val password = signUpBinding.passwordEditText.text.toString().trim()
        val repeatPassword = signUpBinding.repeatPasswordEditText.text.toString().trim()

        return when {
            username.isEmpty() && email.isEmpty() && password.isEmpty() && repeatPassword.isEmpty() -> {
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

            else -> ValidationState.VALID
        }
    }

    private suspend fun performSignUp(): Boolean {
        val username = signUpBinding.nombreEditText.text.toString().trim()
        val email = signUpBinding.emailEditText.text.toString().trim()
        val password = signUpBinding.passwordEditText.text.toString().trim()

        val requestBody = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }

        return try {
            performNetworkOperation("${Network.BASE_URL}/api/auth/register", requestBody)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getSignUpMessages(): Map<ValidationState, String> {
        return mapOf(
            ValidationState.NAME_EMPTY to "Debe ingresar su nombre",
            ValidationState.EMAIL_EMPTY to "Por favor, ingrese su mail.",
            ValidationState.PASSWORD_EMPTY to "Por favor, ingrese su contraseña.",
            ValidationState.ALL_EMPTY to "Por favor, complete sus datos.",
            ValidationState.REPEAT_PASSWORD_EMPTY to "Por favor repita su contraseña",
            ValidationState.NOT_REPEATED_PASSWORD to "Las contraseñas no coinciden",
            ValidationState.INVALID to "Error al crear la cuenta"
        )
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
        signUpBinding.passwordEditText.setSelection(signUpBinding.passwordEditText.length())
        signUpBinding.repeatPasswordEditText.setSelection(signUpBinding.repeatPasswordEditText.length())
        isPasswordVisible = !isPasswordVisible
    }
}
