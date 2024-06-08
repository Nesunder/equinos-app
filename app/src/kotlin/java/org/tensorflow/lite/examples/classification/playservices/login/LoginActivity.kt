package org.tensorflow.lite.examples.classification.playservices.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import org.tensorflow.lite.examples.classification.playservices.MainActivity
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var loginBinding: ActivityLoginBinding

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
            startActivity(mainActivityIntent)
        }

        loginBinding.registerButton.setOnClickListener {
            startActivity(mainActivityIntent)
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
}