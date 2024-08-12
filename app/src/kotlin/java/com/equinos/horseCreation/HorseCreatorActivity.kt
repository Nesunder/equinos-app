package com.equinos.horseCreation

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.equinos.ImageHelper
import com.equinos.R
import com.equinos.appRepository.MainViewModel
import com.equinos.databinding.ActivityHorseCreationBinding
import com.equinos.settings.Network
import java.io.IOException
import java.io.InputStream
import java.util.Calendar


class HorseCreatorActivity : AppCompatActivity() {
    private var imageSelected: Boolean = false
    private val newHorseItem: HorseItem by lazy {
        HorseItem(
            -1, "Nuevo caballo", Uri.parse(
                "android.resource://com.equinos/" + R.drawable.caballo_inicio
            )
        )
    }
    private lateinit var horseCreatorBinding: ActivityHorseCreationBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var imageUri: Uri
    private var dateInput: String = ""
    private val customProgressDialog: Dialog by lazy {
        Dialog(this@HorseCreatorActivity)
    }

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }


    private val openGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    horseCreatorBinding.imgChooser.setImageURI(uri)
                    newHorseItem.imageUri = uri
                    imageSelected = true
                }
            }
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

        horseCreatorBinding.editFechaNacimientoInput.setOnClickListener {
            setDate()
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
            showProgressDialog()
            val nameInput = horseCreatorBinding.editNombreInput.text.toString()
            val observationsInput = horseCreatorBinding.editObservacionesInput.text.toString()
            val trained = horseCreatorBinding.checkboxEntrenado.isChecked
            val withPain = horseCreatorBinding.checkboxDolor.isChecked
            val stabling = horseCreatorBinding.checkboxEstabulacion.isChecked
            val piquete = horseCreatorBinding.checkboxSalidaAPiquete.isChecked

            val caballoJson = buildCaballoJson(
                nameInput,
                sex,
                trained,
                stabling,
                piquete,
                withPain,
                observationsInput,
                dateInput
            )
            if (imageSelected) {
                newHorseItem.name = nameInput
                val bytes: ByteArray? =
                    imageHelper.getByteArrayImage(this@HorseCreatorActivity, imageUri)
                lifecycleScope.launch {
                    val validationState = pushearCaballo(caballoJson, bytes)
                    cancelProgressDialog()
                    if (validationState == ValidationState.INVALID) finish()
                    showDialog(this@HorseCreatorActivity)
                }
            } else {
                cancelProgressDialog()
            }
        }
    }

    enum class ValidationState {
        VALID, INVALID
    }

    private suspend fun pushearCaballo(caballoJson: JSONObject, image: ByteArray?): ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // Construir el cuerpo de la solicitud multipart
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

                // Agregar la parte del JSON del caballo
                val jsonMediaType = "application/json".toMediaTypeOrNull()
                val caballoPart = caballoJson.toString().toRequestBody(jsonMediaType)
                multipartBuilder.addFormDataPart("caballo", "caballo.json", caballoPart)

                // Agregar la parte de la imagen si existe
                image?.let {
                    val imageMediaType = "image/jpeg".toMediaTypeOrNull()
                    val imagePart = it.toRequestBody(imageMediaType)
                    multipartBuilder.addFormDataPart("imagen", "imagen.jpg", imagePart)
                }

                val multipartBody = multipartBuilder.build()

                val token = Network.getAccessToken()
                val request = Request.Builder()
                    .url("${Network.BASE_URL}/api/caballos")
                    .post(multipartBody)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    ValidationState.VALID
                } else {
                    ValidationState.INVALID
                }
            } catch (e: IOException) {
                ValidationState.INVALID
            }
        }
    }

    private fun buildCaballoJson(
        nombre: String,
        sexo: String,
        entrenamiento: Boolean,
        estabulacion: Boolean,
        salidaAPiquete: Boolean,
        dolor: Boolean,
        observaciones: String,
        fechaNacimiento: String
    ): JSONObject {
        val json = JSONObject()

        json.put("nombre", nombre)
        json.put("sexo", sexo)
        json.put("fechaNacimiento", fechaNacimiento)
        json.put("entrenamiento", entrenamiento)
        json.put("estabulacion", estabulacion)
        json.put("salidaAPiquete", salidaAPiquete)
        json.put("dolor", dolor)
        json.put("observaciones", observaciones)

        return json
    }

    private fun openGallery() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(pickIntent)
    }

    // Usado para pruebas
    private fun printImageBytes(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes: ByteArray? = inputStream?.readBytes()

            bytes?.let {
                val encodedImage = Base64.encodeToString(it, Base64.DEFAULT)
                println("Image in bytes: $encodedImage")
            } ?: run {
                Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error al leer la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Carga caballo")
            .setMessage("Carga exitosa")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }.show()
    }

    private fun setDate() {
        val c = Calendar.getInstance()

        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this, { _, pickerYear, monthOfYear, dayOfMonth ->
                val displayDate = "$dayOfMonth/${monthOfYear + 1}/$pickerYear"
                dateInput = "$dayOfMonth-${monthOfYear + 1}-$pickerYear"
                horseCreatorBinding.editFechaNacimientoInput.setText(displayDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showProgressDialog() {
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    private fun cancelProgressDialog() {
        customProgressDialog.dismiss()
    }
}
