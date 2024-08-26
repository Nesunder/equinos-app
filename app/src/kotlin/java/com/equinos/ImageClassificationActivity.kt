package com.equinos

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.equinos.databinding.ActivityImageClassificationBinding
import com.equinos.photoUpload.PhotoUploadFragment


class ImageClassificationActivity : AppCompatActivity() {

    private var savedImagePath: String = ""
    private var classifiedBitmap: Bitmap? = null
    private var result: String = ""
    private var interesadoValue: Float = 0f
    private var serenoValue: Float = 0f
    private var disgustadoValue: Float = 0f
    private lateinit var imageClassificationBinding: ActivityImageClassificationBinding
    private var uri: Uri? = null
    private var classifier: ImageClassificationHelper? = null
    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }
    private var useGpu = false

    private val pickIntent by lazy {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

    private val customProgressDialog: Dialog by lazy {
        Dialog(this@ImageClassificationActivity)
    }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (imageRetrievedCorrectly(result)) {
                uri = result.data?.data!!
                uri?.let { setImage(it) }
            }
        }

    private fun imageRetrievedCorrectly(result: ActivityResult): Boolean {
        return result.resultCode == RESULT_OK && result.data != null && result.data?.data != null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        imageClassificationBinding = ActivityImageClassificationBinding.inflate(layoutInflater)
        setContentView(imageClassificationBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("imageUri", Uri::class.java)
        } else {
            intent.getParcelableExtra("imageUri")
        }

        TFLiteInitializer.getInstance().initializeTask(this).addOnSuccessListener {
            Log.d(TAG, "T|FLite in Play Services initialized successfully.")
            classifier = ImageClassificationHelper(this, MAX_REPORT, useGpu)
            uri?.let { setImage(it) }
        }

        imageClassificationBinding.galleryBtn.setOnClickListener {
            openGalleryLauncher.launch(pickIntent)
        }

        imageClassificationBinding.saveBtn.setOnClickListener {
            saveClassifiedPhoto()
        }

        //agregar una carga o algo para mostrar que está volviendo a calcular
        imageClassificationBinding.reloadBtn.setOnClickListener {
            uri?.let { setImage(it) }
        }

        imageClassificationBinding.uploadBtn.setOnClickListener {
            if (uri != null) {
                showRepositoryFormDialog()
            }
        }

        imageClassificationBinding.galleryBtn.setOnTouchListener { v, event ->
            return@setOnTouchListener buttonAnimation(v, event)
        }
        imageClassificationBinding.saveBtn.setOnTouchListener { v, event ->
            return@setOnTouchListener buttonAnimation(v, event)
        }
        imageClassificationBinding.reloadBtn.setOnTouchListener { v, event ->
            return@setOnTouchListener buttonAnimation(v, event)
        }
        imageClassificationBinding.uploadBtn.setOnTouchListener { v, event ->
            return@setOnTouchListener buttonAnimation(v, event)
        }

    }

    private fun buttonAnimation(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start()
                return true
            }

            MotionEvent.ACTION_UP -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                    Handler(Looper.getMainLooper()).postDelayed({
                        v.performClick() // Ensure accessibility services can handle the click
                    }, 25) // Delay to ensure animation completes
                }.start()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        // Release TFLite resources
        classifier?.clearImageClassifier()
        classifier?.close()
        super.onDestroy()
    }

    private fun setImage(uri: Uri) {
        lifecycleScope.launch {
            showProgressDialog()
            classifiedBitmap = withContext(Dispatchers.IO) {
                imageHelper.getBitmap(uri, contentResolver)
            }
            imageClassificationBinding.imagePredicted.setImageBitmap(classifiedBitmap)

            // Método que funciona bien
            val categories = classifier?.classifyImageManualProcessing(
                classifiedBitmap!!
            )

            if (!categories.isNullOrEmpty()) {
                reportRecognition(categories)
                getPredictionValues(categories)
            }
            cancelProgressDialog()
        }
    }

    private fun getPredictionValues(categories: List<ImageClassificationHelper.Recognition>) {
        result = categories[0].title
        interesadoValue = classifier!!.filterRecognitionsByTitle(categories, "interesado")[0].confidence
        serenoValue = classifier!!.filterRecognitionsByTitle(categories, "sereno")[0].confidence
        disgustadoValue = classifier!!.filterRecognitionsByTitle(categories, "disgustado")[0].confidence
    }

    private fun showRepositoryFormDialog() {
        var classifiedUri: Uri?
        lifecycleScope.launch {
            classifiedUri = imageHelper.getImageUriFromBitmap(
                this@ImageClassificationActivity, imageHelper.getBitmapFromView(
                    imageClassificationBinding.flPreviewViewContainer
                )
            )

            classifiedUri?.let {
                PhotoUploadFragment.newInstance(
                    result, it, interesadoValue, serenoValue, disgustadoValue, savedImagePath
                )
            }?.show(supportFragmentManager, "fragment_photo_upload")
        }
    }

    private fun <T> reportItems(
        items: List<T>?,
        formatItem: (T) -> String
    ) = imageClassificationBinding.viewFinder.post {

        if (items == null || items.size < MAX_REPORT) {
            imageClassificationBinding.textPrediction.visibility = View.GONE
            return@post
        }

        // Update the text and UI
        imageClassificationBinding.textPrediction.text =
            items.subList(0, MAX_REPORT).joinToString(separator = "\n", transform = formatItem)

        // Make sure all UI elements are visible
        imageClassificationBinding.textPrediction.visibility = View.VISIBLE
    }

    private fun reportRecognition(
        recognitions: List<ImageClassificationHelper.Recognition>?
    ) {
        reportItems(recognitions) { recognition ->
            "${"%.2f".format(recognition.confidence)} ${recognition.title}"
        }
    }

    private fun saveClassifiedPhoto() {
        lifecycleScope.launch {
            showProgressDialog()
            savedImagePath = imageHelper.savePhotoFromBitmap(
                this@ImageClassificationActivity,
                imageHelper.getBitmapFromView(
                    imageClassificationBinding.flPreviewViewContainer
                ),
                contentResolver, result
            )

            cancelProgressDialog()
            if (savedImagePath.isNotEmpty()) {
                Toast.makeText(
                    this@ImageClassificationActivity,
                    "Archivo guardado: $savedImagePath",
                    Toast.LENGTH_SHORT
                ).show()
                //shareFile(path)
            } else {
                Toast.makeText(
                    this@ImageClassificationActivity,
                    "Error al guardar el archivo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showProgressDialog() {
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    private fun cancelProgressDialog() {
        customProgressDialog.dismiss()
    }

    companion object {
        val TAG: String? = ImageClassificationActivity::class.java.simpleName
        private const val MAX_REPORT = 3
    }
}