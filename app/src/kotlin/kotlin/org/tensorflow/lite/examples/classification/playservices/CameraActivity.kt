/*
 * Copyright 2022 The TensorFlow Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification.playservices

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityCameraBinding
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random


/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private lateinit var activityCameraBinding: ActivityCameraBinding

    private lateinit var bitmapBuffer: Bitmap

    private val executor = Executors.newSingleThreadExecutor()

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing
        get() = lensFacing == CameraSelector.LENS_FACING_FRONT

    private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private var useGpu = false

    // Initialize TFLite once. Must be called before creating the classifier
    private val initializeTask: Task<Void> by lazy {
        TfLite.initialize(
            this, TfLiteInitializationOptions.builder().setEnableGpuDelegateSupport(true).build()
        ).continueWithTask { task ->
            if (task.isSuccessful) {
                useGpu = true
                return@continueWithTask Tasks.forResult(null)
            } else {
                // Fallback to initialize interpreter without GPU
                return@continueWithTask TfLite.initialize(this)
            }
        }.addOnFailureListener {
            Log.e(TAG, "TFLite in Play Services failed to initialize.", it)
        }
    }
    private var classifier: ImageClassificationHelper? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null && result.data?.data != null) {
                if (!pauseAnalysis) pauseAnalysis = true
                //Acá se podría abrir una actividad nueva que tenga una interfaz más dedidcada a la clasificación de una imagen cargada, con la posibilidad de guardar,
                // reemplazar la imagen, subirla al repo
                val uri = result.data?.data!!
                val imageBitmap: Bitmap? = getBitmapFromUri(uri)

                val inputStream = contentResolver.openInputStream(uri)

                //Obtengo la orientación de la imagen para dejarla derecha, así solo funciona desde api 24
                val exifInterface = inputStream?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ExifInterface(it)
                    } else {
                        TODO("VERSION.SDK_INT < N")
                    }
                }

                val orientation = exifInterface?.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )

                //Se aplican cambios en la imagen
                val rotatedBitmap = imageBitmap?.let {
                    configBitmap(
                        it, orientation ?: ExifInterface.ORIENTATION_NORMAL
                    )
                }

                activityCameraBinding.imagePredicted.setImageBitmap(rotatedBitmap)

                val recognitions = classifier?.classify(
                    rotatedBitmap!!, activityCameraBinding.imagePredicted.rotation.toInt()
                )

                reportRecognition(recognitions)

                activityCameraBinding.imagePredicted.visibility = View.VISIBLE
                activityCameraBinding.viewFinder.visibility = View.GONE
                activityCameraBinding.saveButton?.visibility = View.VISIBLE
            } else {
                pauseAnalysis = false
                activityCameraBinding.imagePredicted.visibility = View.GONE
                activityCameraBinding.viewFinder.visibility = View.VISIBLE
                activityCameraBinding.saveButton?.visibility = View.GONE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(activityCameraBinding.root)

        // Initialize TFLite asynchronously
        initializeTask.addOnSuccessListener {
            Log.d(TAG, "TFLite in Play Services initialized successfully.")
            classifier = ImageClassificationHelper(this, MAX_REPORT, useGpu)
        }

        /* Cosas a agregar:
                * Guardar la foto
                * La interfaz inicial no debería ser la cámara, sino que sería una opción. También sería una opción subir una foto y clasificarla,
                  pudiendo guardarla localmente y en el repo.
                * También debería haber una opción desde la interfaz inicial de ver todas las fotos guardadas localmente.
                * Botón para redirigir a la página del repo
                * Calcular en el momento de sacar la foto para comparar el resultado
                * Cargar foto y que la evalúe.
                * Subir foto al repositorio como un usuario
                * Login y autentificación
                * cálculo en server?
         */

        activityCameraBinding.galleryButton?.setOnClickListener {
            //ver el tema de permisos mejor
            openGallery()
        }

        activityCameraBinding.cameraCaptureButton.setOnClickListener {
            // Disable all camera controls
            it.isEnabled = false
            if (pauseAnalysis) {
                // If image analysis is in paused state, resume it
                pauseAnalysis = false
                activityCameraBinding.imagePredicted.visibility = View.GONE
                activityCameraBinding.viewFinder.visibility = View.VISIBLE
                activityCameraBinding.saveButton?.visibility = View.GONE
            } else {
                // Otherwise, pause image analysis and freeze image
                pauseAnalysis = true
                val matrix = Matrix().apply {
                    postRotate(imageRotationDegrees.toFloat())
                    if (isFrontFacing) postScale(-1f, 1f)
                }
                val uprightImage = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
                )
                activityCameraBinding.imagePredicted.setImageBitmap(uprightImage)
                activityCameraBinding.imagePredicted.visibility = View.VISIBLE
                activityCameraBinding.saveButton?.visibility = View.VISIBLE
            }

            // Re-enable camera controls
            it.isEnabled = true
        }

        activityCameraBinding.saveButton?.setOnClickListener {
            lifecycleScope.launch {
                saveBitmapFile(getBitmapFromView(activityCameraBinding.flPreviewViewContainer!!))
            }
        }
    }

    override fun onDestroy() {
        // Terminate all outstanding analyzing jobs (if there is any).
        executor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
        // Release TFLite resources
        classifier?.close()
        super.onDestroy()
    }

    /**
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw black background on the canvas
            canvas.drawColor(Color.BLACK)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var path = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "EquinosApp_" + System.currentTimeMillis() / 1000
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/EquinosApp")
                    }
                    val uri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                    )

                    path = uri?.path!!

                    contentResolver.openOutputStream(uri).use { outputStream ->
                        // Use the outputStream to save your bitmap
                        if (outputStream != null) {
                            mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        }
                    }

                    runOnUiThread {
                        //cancelProgressDialog()
                        if (path.isNotEmpty()) {
                            Toast.makeText(
                                this@CameraActivity,
                                "File saved successfully: $path",
                                Toast.LENGTH_SHORT
                            ).show()
                            //shareFile(path)
                        } else {
                            Toast.makeText(
                                this@CameraActivity,
                                "Error while saving the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    path = ""
                    e.printStackTrace()
                }
            }
        }
        return path
    }

    private fun configBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            if (isFrontFacing) postScale(-1f, 1f)
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> this.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> this.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    setRotate(180f)
                    this.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                else -> return bitmap
            }
        }
        return try {
            val oriented =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            oriented
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun openGallery() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(pickIntent)
    }

    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = activityCameraBinding.viewFinder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // Camera provider is now guaranteed to be available
                val cameraProvider = cameraProviderFuture.get()

                // Set up the view finder use case to display camera preview
                // Con esto se puede cambiar la relación de aspecto.
                // Deprecado en versiones mas nuevas de android, en una app que hice, esta hecho de otra forma.
                val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setTargetRotation(activityCameraBinding.viewFinder.display.rotation).build()

                // Set up the image analysis use case which will process frames in real time
                val imageAnalysis =
                    ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(activityCameraBinding.viewFinder.display.rotation)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888).build()

                var frameCounter = 0
                var lastFpsTimestamp = System.currentTimeMillis()

                imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        // The image rotation and RGB image buffer are initialized only once
                        // the analyzer has started running
                        imageRotationDegrees = image.imageInfo.rotationDegrees
                        bitmapBuffer = Bitmap.createBitmap(
                            image.width, image.height, Bitmap.Config.ARGB_8888
                        )
                    }

                    // Early exit: image analysis is in paused state, or TFLite is not initialized
                    if (pauseAnalysis || classifier == null) {
                        image.close()
                        return@Analyzer
                    }

                    // Copy out RGB bits to our shared buffer
                    image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                    // Perform the image classification for the current frame
                    val recognitions = classifier?.classify(bitmapBuffer, imageRotationDegrees)

                    reportRecognition(recognitions)

                    // Compute the FPS of the entire pipeline
                    val frameCount = 10
                    if (++frameCounter % frameCount == 0) {
                        frameCounter = 0
                        val now = System.currentTimeMillis()
                        val delta = now - lastFpsTimestamp
                        val fps = 1000 * frameCount.toFloat() / delta
                        Log.d(TAG, "FPS: ${"%.02f".format(fps)}")
                        lastFpsTimestamp = now
                    }
                })

                // Create a new camera selector each time, enforcing lens facing
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                // Apply declared configs to CameraX using the same lifecycle owner
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis
                )

                // Use the camera object to link our preview use case with the view
                preview.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)
            }, ContextCompat.getMainExecutor(this)
        )
    }

    /** Displays recognition results on screen. */
    private fun reportRecognition(
        recognitions: List<ImageClassificationHelper.Recognition>?,
    ) = activityCameraBinding.viewFinder.post {

        // Early exit: if recognition is null, or there are not enough recognition results.
        if (recognitions == null || recognitions.size < MAX_REPORT) {
            activityCameraBinding.textPrediction.visibility = View.GONE
            return@post
        }

        // Update the text and UI
        activityCameraBinding.textPrediction.text =
            recognitions.subList(0, MAX_REPORT).joinToString(separator = "\n") {
                "${"%.2f".format(it.confidence)} ${it.title}"
            }

        // Make sure all UI elements are visible
        activityCameraBinding.textPrediction.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        } else {
            bindCameraUseCases()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            bindCameraUseCases()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val TAG = CameraActivity::class.java.simpleName
        private const val MAX_REPORT = 3
    }
}
