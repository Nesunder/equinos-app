package org.tensorflow.lite.examples.classification.playservices.loginSignUp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import org.tensorflow.lite.examples.classification.playservices.MainActivity
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var loginBinding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(loginBinding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setWindowsInsets()
        }

        val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
        loginBinding.loginButton.setOnClickListener {
            val validationState = validateInputs()
            if (validationState == ValidationState.VALID) {
                startActivity(mainActivityIntent)
            } else {
                showValidationDialog(validationState)
            }
        }

        val signUpActivityIntent = Intent(applicationContext, SignUpActivity::class.java)
        loginBinding.registerButton.setOnClickListener {
            startActivity(signUpActivityIntent)
        }

        loginBinding.passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setWindowsInsets() {
        var hasGestureNavigation: Boolean
        loginBinding.root.doOnAttach { view ->
            val insets = view.rootWindowInsets?.getInsets(WindowInsets.Type.systemGestures())
            hasGestureNavigation = insets?.let {
                it.left > 0 || it.right > 0
            } ?: false

            if (hasGestureNavigation) {
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insetsBars ->
                    val systemBars = insetsBars.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(
                        systemBars.left, systemBars.top, systemBars.right, systemBars.bottom
                    )
                    insetsBars
                }
            }
        }

    }

    private enum class ValidationState {
        VALID, EMAIL_EMPTY, PASSWORD_EMPTY, BOTH_EMPTY
    }

    private fun validateInputs(): ValidationState {
        val email = loginBinding.emailEditText.text.toString().trim()
        val password = loginBinding.passwordEditText.text.toString().trim()

        return when {
            password.isEmpty() && email.isEmpty() -> {
                loginBinding.emailEditText.error = "Se debe ingresar el mail"
                loginBinding.passwordEditText.error = "Se debe ingresar la contraseña"
                ValidationState.BOTH_EMPTY
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

    private fun showValidationDialog(validationState: ValidationState) {
        val message = when (validationState) {
            ValidationState.EMAIL_EMPTY -> "Por favor, ingrese su mail."
            ValidationState.PASSWORD_EMPTY -> "Por favor, ingrese su contraseña."
            ValidationState.BOTH_EMPTY -> "Por favor, complete sus datos."
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