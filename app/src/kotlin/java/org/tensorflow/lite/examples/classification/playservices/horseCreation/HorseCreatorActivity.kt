package org.tensorflow.lite.examples.classification.playservices.horseCreation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.examples.classification.playservices.ImageHelper
import org.tensorflow.lite.examples.classification.playservices.R
import org.tensorflow.lite.examples.classification.playservices.appRepository.MainViewModel
import org.tensorflow.lite.examples.classification.playservices.databinding.ActivityHorseCreationBinding

class HorseCreatorActivity : AppCompatActivity() {

    private var imageSelected: Boolean = false
    private val newHorseItem: HorseItem by lazy {
        HorseItem(
            "Nuevo caballo", BitmapFactory.decodeResource(
                resources, R.drawable.caballo_inicio
            )
        )
    }
    private lateinit var horseCreatorBinding: ActivityHorseCreationBinding
    private lateinit var viewModel: MainViewModel
    private var imageBitmap: Bitmap? = null

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    private val pickIntent by lazy {
        Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (imageRetrievedCorrectly(result)) {
                val uri = result.data?.data!!

                lifecycleScope.launch {
                    imageBitmap = withContext(Dispatchers.IO) {
                        imageHelper.getBitmap(uri, contentResolver)
                    }
                    horseCreatorBinding.imgChooser.setImageBitmap(imageBitmap)
                    newHorseItem.imageBitmap = imageBitmap!!
                    imageSelected = true
                }

            }
        }

    private fun imageRetrievedCorrectly(result: ActivityResult): Boolean {
        return result.resultCode == RESULT_OK && result.data != null && result.data?.data != null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        horseCreatorBinding = ActivityHorseCreationBinding.inflate(layoutInflater)
        setContentView(horseCreatorBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        horseCreatorBinding.imgChooserView.setOnClickListener {
            openGallery()
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        horseCreatorBinding.cancelButton.setOnClickListener {
            finish()
        }

        var sex = "Masculino"

        // Listener para obtener el valor del spinner
        horseCreatorBinding.spinnerSexo.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: android.view.View, position: Int, id: Long
                ) {
                    sex = parent.getItemAtPosition(position).toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    return
                }
            }

        horseCreatorBinding.confirmButton.setOnClickListener {
            //obtener datos del formulario y armar un objeto para mandarlo
            val nameInput = horseCreatorBinding.editNombreInput.text.toString()
            val ageInput = horseCreatorBinding.editNombreInput.text.toString()
            val observationsInput = horseCreatorBinding.editObservacionesInput.text.toString()
            val trained = horseCreatorBinding.checkboxEntrenado.isChecked
            val withPain = horseCreatorBinding.checkboxDolor.isChecked
            val stabling = horseCreatorBinding.checkboxEstabulacion.isChecked
            // Todavía no se que es esto
            val piquete = horseCreatorBinding.checkboxSalidaAPiquete.isChecked

            if (imageBitmap != null) {
                //para subir la imagen
                newHorseItem.text = nameInput
                viewModel.addItem(newHorseItem)
            }
            finish()
        }
    }

    private fun openGallery() {
        openGalleryLauncher.launch(pickIntent)
    }
}