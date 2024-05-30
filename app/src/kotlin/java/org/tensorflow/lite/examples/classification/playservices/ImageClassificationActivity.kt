package org.tensorflow.lite.examples.classification.playservices

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityImageClassificationBinding
import org.tensorflow.lite.support.label.Category

class ImageClassificationActivity : AppCompatActivity() {


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

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null && result.data?.data != null) {
                uri = result.data?.data!!
                uri?.let { setImage(it) }
            }
        }

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

        imageClassificationBinding.shareBtn.setOnClickListener {
            showRepositoryFormDialog()
        }


    }

    override fun onDestroy() {
        // Release TFLite resources
        classifier?.clearImageClassifier()
        classifier?.close()
        super.onDestroy()
    }

    private fun setImage(uri: Uri) {
        val imageBitmap: Bitmap? = imageHelper.getBitmap(uri, contentResolver)
        imageClassificationBinding.imagePredicted.setImageBitmap(imageBitmap)

        // Método que funciona bien
        val recognitions = classifier?.classifyImageManualProcessing(
            imageBitmap!!
        )
        reportRecognition(recognitions)

        // Para clasificar con el otro método
        /*
        val categories = classifier?.classifyWithMetadata(
        imageBitmap!!, imageClassificationBinding.imagePredicted.rotation.toInt()
        )
        reportCategories(categories)
        */
    }

    private fun showRepositoryFormDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.horse_data_form, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val alertDialog = builder.show()

        val cancelButton: Button = dialogView.findViewById(R.id.buttonCancel)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
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

    private fun reportCategories(
        categories: List<Category>?
    ) {
        reportItems(categories) { category ->
            "${"%.2f".format(category.score)} ${category.label}"
        }
    }

    private fun saveClassifiedPhoto() {
        lifecycleScope.launch {

            val path = imageHelper.saveBitmapFile(
                imageHelper.getBitmapFromView(
                    imageClassificationBinding.flPreviewViewContainer
                ),
                contentResolver
            )

            //cancelProgressDialog()
            if (path.isNotEmpty()) {
                Toast.makeText(
                    this@ImageClassificationActivity,
                    "Archivo guardado: $path",
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

    companion object {
        val TAG: String? = ImageClassificationActivity::class.java.simpleName
        private const val MAX_REPORT = 3
    }
}