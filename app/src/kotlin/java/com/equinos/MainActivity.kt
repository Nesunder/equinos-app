package com.equinos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.equinos.databinding.MainActivityBinding
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var mainActivityBinding: MainActivityBinding

    private val permissions by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private val permissionsRequestCode = Random.nextInt(0, 10000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainActivityBinding = MainActivityBinding.inflate(layoutInflater)
        setContentView(mainActivityBinding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setWindowsInsets()
        }

        val navView: BottomNavigationView = mainActivityBinding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navView.setupWithNavController(navHostFragment.navController)

        val openCameraIntent = Intent(applicationContext, CameraActivity::class.java)
        mainActivityBinding.cameraButton.setOnClickListener {
            startActivity(openCameraIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setWindowsInsets() {
        var hasGestureNavigation: Boolean
        mainActivityBinding.root.doOnAttach { view ->
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            return
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}