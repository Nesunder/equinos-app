package org.tensorflow.lite.examples.classification.playservices.loginSignUp

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivitySignUpBinding


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
            when (val validationState = validateInputs()) {
                ValidationState.VALID -> {
                    startActivity(loginActivityIntent)
                    finish()
                }

                ValidationState.NOT_REPEATED_PASSWORD -> {
                    togglePasswordVisibility()
                }

                else -> {
                    showValidationDialog(validationState)
                }
            }
        }
        signUpBinding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

    }

    private enum class ValidationState {
        VALID, NAME_EMPTY, EMAIL_EMPTY, PASSWORD_EMPTY, REPEAT_PASSWORD_EMPTY, ALL_EMPTY, NOT_REPEATED_PASSWORD
    }

    private fun validateInputs(): ValidationState {
        val name = signUpBinding.nombreEditText.text.toString().trim()
        val email = signUpBinding.emailEditText.text.toString().trim()
        val password = signUpBinding.passwordEditText.text.toString().trim()
        val repeatPassword = signUpBinding.repeatPasswordEditText.text.toString().trim()

        return when {
            password.isEmpty() && email.isEmpty() && name.isEmpty() && repeatPassword.isEmpty() -> {
                signUpBinding.nombreEditText.error = "Debe ingresar su nombre"
                signUpBinding.emailEditText.error = "Se debe ingresar el mail"
                signUpBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                signUpBinding.repeatPasswordEditText.error = "Por favor repita su contraseña"
                ValidationState.ALL_EMPTY
            }

            name.isEmpty() -> {
                signUpBinding.nombreEditText.error = "Debe ingresar su nombre"
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

    private fun showValidationDialog(validationState: ValidationState) {
        val message = when (validationState) {
            ValidationState.NAME_EMPTY -> "Debe ingresar su nombre"
            ValidationState.EMAIL_EMPTY -> "Por favor, ingrese su mail."
            ValidationState.PASSWORD_EMPTY -> "Por favor, ingrese su contraseña."
            ValidationState.ALL_EMPTY -> "Por favor, complete sus datos."
            ValidationState.REPEAT_PASSWORD_EMPTY -> "Por favor repita su contraseña"
            ValidationState.NOT_REPEATED_PASSWORD -> return
            ValidationState.VALID -> return

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