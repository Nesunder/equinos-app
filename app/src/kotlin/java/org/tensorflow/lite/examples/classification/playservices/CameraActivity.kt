package org.tensorflow.lite.examples.classification.playservices

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityCameraBinding
import org.tensorflow.lite.examples.classification.playservices.photoUpload.PhotoUploadFragment
import org.tensorflow.lite.support.label.Category
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private lateinit var preview: Preview
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var activityCameraBinding: ActivityCameraBinding

    private lateinit var bitmapBuffer: Bitmap

    private val executor = Executors.newSingleThreadExecutor()

    private val permissions by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private val pickIntent by lazy {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing
        get() = lensFacing == CameraSelector.LENS_FACING_FRONT

    private var pauseAnalysis = false
    private var imageRotationDegrees: Int = 0
    private var useGpu = false

    private var classifier: ImageClassificationHelper? = null

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null && result.data?.data != null) {
                if (!pauseAnalysis) pauseAnalysis = true
                //Acá se podría abrir una actividad nueva que tenga una interfaz más dedidcada a la clasificación de una imagen cargada, con la posibilidad de guardar,
                // reemplazar la imagen, subirla al repo
                val uri = result.data?.data!!
                var imageBitmap: Bitmap?
                lifecycleScope.launch {
                    imageBitmap = withContext(Dispatchers.IO) {
                        imageHelper.getBitmap(uri, contentResolver)
                    }

                    activityCameraBinding.imagePredicted.setImageBitmap(imageBitmap)

                    val recognition = classifier?.classifyImageManualProcessing(
                        imageBitmap!!
                    )

                    reportRecognition(recognition)
                    setPredictedView()
                    bindCameraUseCases()
                }

            } else {
                pauseAnalysis = false
                setAnalysisView()
                bindCameraUseCases()
            }
        }

    private fun setPredictedView() {
        activityCameraBinding.imagePredicted.visibility = View.VISIBLE
        activityCameraBinding.viewFinder.visibility = View.GONE
        activityCameraBinding.saveButton?.visibility = View.VISIBLE
        activityCameraBinding.uploadBtn?.visibility = View.VISIBLE
    }

    private fun setAnalysisView() {
        activityCameraBinding.imagePredicted.visibility = View.GONE
        activityCameraBinding.viewFinder.visibility = View.VISIBLE
        activityCameraBinding.saveButton?.visibility = View.GONE
        activityCameraBinding.uploadBtn?.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityCameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(activityCameraBinding.root)

        // Initialize TFLite asynchronously
        TFLiteInitializer.getInstance().initializeTask(this).addOnSuccessListener {
            Log.d(TAG, "TFLite in Play Services initialized successfully.")
            classifier = ImageClassificationHelper(this, MAX_REPORT, useGpu)
        }

        onBackPressedDispatcher.addCallback {
            classifier?.clearImageClassifier()
            cameraProvider.unbind(preview)
            finish()
        }

        activityCameraBinding.galleryButton?.setOnClickListener {
            pauseAnalysis = true
            openGallery()
        }

        activityCameraBinding.cameraCaptureButton.setOnClickListener {
            // Disable all camera controls
            it.isEnabled = false
            if (pauseAnalysis) {
                // If image analysis is in paused state, resume it
                pauseAnalysis = false
                setAnalysisView()
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
                activityCameraBinding.uploadBtn?.visibility = View.VISIBLE
            }

            // Re-enable camera controls
            it.isEnabled = true
        }

        activityCameraBinding.saveButton?.setOnClickListener {
            lifecycleScope.launch {
                saveClassifiedPhoto()
            }
        }

        activityCameraBinding.uploadBtn?.setOnClickListener {
            showRepositoryFormDialog()
        }
    }

    override fun onDestroy() {
        // Terminate all outstanding analyzing jobs (if there is any).
        executor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
        // Release TFLite resources
        classifier?.clearImageClassifier()
        classifier?.close()
        super.onDestroy()
    }

    private fun openGallery() {
        cameraProvider.unbind(preview)
        openGalleryLauncher.launch(pickIntent)
    }

    private fun saveClassifiedPhoto() {
        lifecycleScope.launch {

            val path = imageHelper.saveBitmapFile(
                imageHelper.getBitmapFromView(
                    activityCameraBinding.flPreviewViewContainer!!
                ),
                contentResolver
            )

            //cancelProgressDialog()
            if (path.isNotEmpty()) {
                Toast.makeText(
                    this@CameraActivity,
                    "Archivo guardado: $path",
                    Toast.LENGTH_SHORT
                ).show()
                //shareFile(path)
            } else {
                Toast.makeText(
                    this@CameraActivity,
                    "Error al guardar el archivo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showRepositoryFormDialog() {
        val fragmentManager = supportFragmentManager
        val newFragment = PhotoUploadFragment()
        newFragment.show(fragmentManager, "fragment_photo_upload")
    }

    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = activityCameraBinding.viewFinder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // Camera provider is now guaranteed to be available
                cameraProvider = cameraProviderFuture.get()

                // Set up the view finder use case to display camera preview
                // Con esto se puede cambiar la relación de aspecto.
                // Deprecado en versiones mas nuevas de android, en una app que hice, esta hecho de otra forma.
                preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
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

                    val categories =
                        classifier?.classifyImageManualProcessing(bitmapBuffer)

                    reportRecognition(categories)

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

    private fun <T> reportItems(
        items: List<T>?,
        formatItem: (T) -> String
    ) = activityCameraBinding.viewFinder.post {

        if (items == null || items.size < MAX_REPORT) {
            activityCameraBinding.textPrediction.visibility = View.GONE
            return@post
        }

        // Update the text and UI
        activityCameraBinding.textPrediction.text =
            items.subList(0, MAX_REPORT).joinToString(separator = "\n", transform = formatItem)

        // Make sure all UI elements are visible
        activityCameraBinding.textPrediction.visibility = View.VISIBLE
    }

    private fun reportRecognition(
        recognitions: List<ImageClassificationHelper.Recognition>?
    ) {
        reportItems(recognitions) { recognition ->
            "${"%.2f".format(recognition.confidence)} ${recognition.title}"
        }
    }

    private fun reportCategories(
        categories: List<Category>?
    ) {
        reportItems(categories) { category ->
            "${"%.2f".format(category.score)} ${category.label}"
        }
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
