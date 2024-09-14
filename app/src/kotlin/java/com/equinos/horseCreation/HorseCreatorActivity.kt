package com.equinos.horseCreation

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
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
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import com.equinos.ImageHelper
import com.equinos.R
import com.equinos.appRepository.MainViewModel
import com.equinos.databinding.ActivityHorseCreationBinding
import com.equinos.settings.Network
import java.io.IOException
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
                    val validationState = uploadHorse(caballoJson, bytes)
                    cancelProgressDialog()
                    if (validationState == Network.ValidationState.INVALID) finish()
                    showDialog(this@HorseCreatorActivity)
                }
            } else {
                cancelProgressDialog()
            }
        }
    }

    private suspend fun uploadHorse(caballoJson: JSONObject, image: ByteArray?): Network.ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("${Network.BASE_URL}/api/horses")
                val connection = url.openConnection() as HttpURLConnection
                val token = Network.getAccessToken()
                val boundary = "Boundary-" + System.currentTimeMillis()
                val lineEnd = "\r\n"

                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                val outputStream = DataOutputStream(connection.outputStream)

                outputStream.writeBytes("--$boundary$lineEnd")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"horse\"; filename=\"caballo.json\"$lineEnd")
                outputStream.writeBytes("Content-Type: application/json$lineEnd")
                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(caballoJson.toString())
                outputStream.writeBytes(lineEnd)

                if (image != null) {
                    outputStream.writeBytes("--$boundary$lineEnd")
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"imagen.jpg\"$lineEnd")
                    outputStream.writeBytes("Content-Type: image/jpeg$lineEnd")
                    outputStream.writeBytes(lineEnd)
                    outputStream.write(image)
                    outputStream.writeBytes(lineEnd)
                }

                outputStream.writeBytes("--$boundary--$lineEnd")
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode

                if (responseCode in 200..299) {
                    Network.ValidationState.VALID
                } else {
                    Network.ValidationState.INVALID
                }

            } catch (e: IOException) {
                Network.ValidationState.INVALID
            }
        }
    }

    private fun buildCaballoJson(
        name: String,
        gender: String,
        entrenamiento: Boolean,
        estabulacion: Boolean,
        salidaAPiquete: Boolean,
        dolor: Boolean,
        observations: String,
        dateOfBirth: String
    ): JSONObject {
        val json = JSONObject()

        json.put("name", name)
        json.put("gender", gender)
        json.put("dateOfBirth", dateOfBirth)
        json.put("entrenamiento", entrenamiento)
        json.put("estabulacion", estabulacion)
        json.put("salidaAPiquete", salidaAPiquete)
        json.put("dolor", dolor)
        json.put("observations", observations)

        return json
    }

    private fun openGallery() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        openGalleryLauncher.launch(pickIntent)
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
