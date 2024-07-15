package com.equinos

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
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
import com.equinos.databinding.ActivityCameraBinding
import com.equinos.photoUpload.PhotoUploadFragment
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import android.animation.AnimatorSet
import android.app.Dialog
import android.view.animation.AccelerateDecelerateInterpolator

/** Activity that displays the camera and performs object detection on the incoming frames */
class CameraActivity : AppCompatActivity() {

    private var savedImagePath: String = ""
    private lateinit var imageAnalysis: ImageAnalysis
    private var uri: Uri? = null
    private var predictionResult: String = ""
    private var interesadoValue: Float = 0f
    private var serenoValue: Float = 0f
    private var disgustadoValue: Float = 0f
    private lateinit var preview: Preview
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var activityCameraBinding: ActivityCameraBinding
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var cameraControl: CameraControl
    private val customProgressDialog: Dialog by lazy {
        Dialog(this@CameraActivity)
    }

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
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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
            if (imageRetrievedCorrectly(result)) {
                showProgressDialog()
                if (!pauseAnalysis) pauseAnalysis = true
                uri = result.data?.data!!
                var imageBitmap: Bitmap?
                lifecycleScope.launch {
                    imageBitmap = withContext(Dispatchers.IO) {
                        imageHelper.getBitmap(uri!!, contentResolver)
                    }

                    imageBitmap?.let { classifyAndSetPredictedImage(it) }
                    setPredictedView()
                    bindCameraUseCases()
                    cancelProgressDialog()
                }

            } else {
                pauseAnalysis = false
                setAnalysisView()
                bindCameraUseCases()
            }
        }

    private fun imageRetrievedCorrectly(result: ActivityResult): Boolean {
        return result.resultCode == RESULT_OK && result.data != null && result.data?.data != null
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

                // Capture bitmap from PreviewView
                val unpredictedImage = activityCameraBinding.viewFinder.bitmap
                if (unpredictedImage == null) {
                    it.isEnabled = true
                    return@setOnClickListener
                }

                val matrix = Matrix().apply {
                    if (isFrontFacing) postScale(-1f, 1f)
                }
                val correctedImage = Bitmap.createBitmap(
                    unpredictedImage, 0, 0, unpredictedImage.width, unpredictedImage.height, matrix, true
                )

                classifyAndSetPredictedImage(correctedImage)
                activityCameraBinding.imagePredicted.visibility = View.VISIBLE
                activityCameraBinding.saveButton?.visibility = View.VISIBLE
                activityCameraBinding.uploadBtn?.visibility = View.VISIBLE

                lifecycleScope.launch {
                    uri = imageHelper.getImageUriFromBitmap(
                        this@CameraActivity, correctedImage
                    )
                }
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
            if (uri != null) {
                showRepositoryFormDialog()
            }
        }

        activityCameraBinding.viewFinder.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val meteringPointFactory = activityCameraBinding.viewFinder.meteringPointFactory
                val meteringPoint = meteringPointFactory.createPoint(event.x, event.y)
                val focusAction = FocusMeteringAction.Builder(meteringPoint).build()
                v.performClick()
                // Show animation
                showFocusAnimation(event.x, event.y)

                // Perform focus action
                cameraControl.startFocusAndMetering(focusAction)
            }
            return@setOnTouchListener true
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

    private suspend fun saveClassifiedPhoto() {
        showProgressDialog()
        savedImagePath = imageHelper.savePhotoFromBitmap(
            this@CameraActivity, imageHelper.getBitmapFromView(
                activityCameraBinding.flPreviewViewContainer!!
            ), contentResolver, predictionResult
        )

        cancelProgressDialog()
        if (savedImagePath.isNotEmpty()) {
            Toast.makeText(
                this@CameraActivity, "Archivo guardado: $savedImagePath", Toast.LENGTH_SHORT
            ).show()
            //shareFile(path)
        } else {
            Toast.makeText(
                this@CameraActivity, "Error al guardar el archivo", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showRepositoryFormDialog() {
        val newFragment = PhotoUploadFragment.newInstance(
            predictionResult, uri!!, interesadoValue, serenoValue, disgustadoValue, savedImagePath
        )
        newFragment.show(supportFragmentManager, "fragment_photo_upload")
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
                imageAnalysis =
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

                    val categories = classifier?.classifyImageManualProcessing(bitmapBuffer)

                    reportRecognition(categories)

                    if (!categories.isNullOrEmpty()) {
                        getPredictionValues(categories)
                    }

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
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis
                )

                cameraControl = camera.cameraControl
                // Use the camera object to link our preview use case with the view
                preview.setSurfaceProvider(activityCameraBinding.viewFinder.surfaceProvider)
            }, ContextCompat.getMainExecutor(this)
        )
    }

    private fun <T> reportItems(
        items: List<T>?, formatItem: (T) -> String
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

    private fun classifyAndSetPredictedImage(bitmap: Bitmap) {
        activityCameraBinding.imagePredicted.setImageBitmap(bitmap)

        // Perform classification on the captured image
        val categories = classifier?.classifyImageManualProcessing(bitmap)

        if (!categories.isNullOrEmpty()) {
            reportRecognition(categories)
            getPredictionValues(categories)
        }
    }

    private fun getPredictionValues(categories: List<ImageClassificationHelper.Recognition>) {
        predictionResult = categories[0].title
        interesadoValue = classifier!!.filterRecognitionsByTitle(categories, "interesado")[0].confidence
        serenoValue = classifier!!.filterRecognitionsByTitle(categories, "sereno")[0].confidence
        disgustadoValue = classifier!!.filterRecognitionsByTitle(categories, "disgustado")[0].confidence
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

    private fun showFocusAnimation(x: Float, y: Float) {
        val focusView = activityCameraBinding.focusView!!
        focusView.x = x - focusView.width / 2
        focusView.y = y - focusView.height / 2
        focusView.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(focusView.x, "scaleX", 1.5f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(focusView.y, "scaleY", 1.5f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(focusView.visibility, "alpha", 0.8f, 0.0f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }

        animatorSet.start()
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                focusView.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun showProgressDialog() {
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    private fun cancelProgressDialog() {
        customProgressDialog.dismiss()
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
