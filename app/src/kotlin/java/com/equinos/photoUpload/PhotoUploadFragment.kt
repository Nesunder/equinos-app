package com.equinos.photoUpload

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.equinos.appRepository.DataRepository
import com.equinos.appRepository.MainViewModel
import com.equinos.databinding.FragmentPhotoUploadBinding
import com.equinos.gallery.ImageInfo
import com.equinos.horseCreation.HorseItem
import com.equinos.horseCreation.HorseCreatorActivity
import com.equinos.horseCreation.HorseItemAdapter
import com.equinos.settings.Network
import java.io.IOException


class PhotoUploadFragment : DialogFragment() {

    private var imageUri: Uri? = null
    private var _binding: FragmentPhotoUploadBinding? = null
    private val TAG = PhotoUploadFragment::class.java.simpleName

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var adapter: HorseItemAdapter
    private var isDropdownVisible = false
    private lateinit var viewModel: MainViewModel
    private var selectedHorse: HorseItem? = null

    private val imageHelper: ImageHelper by lazy {
        ImageHelper()
    }

    private val customProgressDialog: Dialog by lazy {
        Dialog(requireContext())
    }

    companion object {
        private const val ARG_PREDICTION = "arg_text"
        private const val ARG_URI = "arg_uri"
        private const val ARG_SERENO = "arg_interesado"
        private const val ARG_INTERESADO = "arg_sereno"
        private const val ARG_DISGUSTADO = "arg_disgustado"
        private const val ARG_IMAGE_PATH = "arg_path"


        fun newInstance(
            prediction: String,
            uri: Uri,
            interesadoValue: Float,
            serenoValue: Float,
            disgustadoValue: Float,
            savedImagePath: String
        ): PhotoUploadFragment {
            val fragment = PhotoUploadFragment()
            val args = Bundle()
            args.putString(ARG_PREDICTION, prediction)
            args.putString(ARG_IMAGE_PATH, savedImagePath)
            args.putFloat(ARG_INTERESADO, interesadoValue)
            args.putFloat(ARG_DISGUSTADO, disgustadoValue)
            args.putFloat(ARG_SERENO, serenoValue)
            args.putParcelable(ARG_URI, uri)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val initialData = context?.let { it1 -> DataRepository.loadInitialData() }
            initialData?.let {
                viewModel.updateData(it)
                adapter.updateData(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupView() {
        val prediction = arguments?.getString(ARG_PREDICTION)

        imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_URI, Uri::class.java)
        } else {
            arguments?.getParcelable(ARG_URI)
        }

        binding.predictionText.text = prediction
        binding.predictedImage.setImageURI(imageUri)

        setupRecyclerView()

        binding.toggleButton.setOnClickListener {
            // Se llama cada vez que se abre
            toggleDropdown()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.confirmButton.setOnClickListener {
            prediction?.let { it1 -> confirmPhotoUpload(it1) }
        }

        val horseCreatorActivity = Intent(context, HorseCreatorActivity::class.java)
        binding.addHorseBtn.setOnClickListener {
            startActivity(horseCreatorActivity)
        }
    }

    private fun confirmPhotoUpload(prediction: String) {
        if (selectedHorse != null) {
            showProgressDialog()
            lifecycleScope.launch {
                val validationState = uploadPhoto(
                    prediction
                )
                cancelProgressDialog()
                if (validationState == HorseCreatorActivity.ValidationState.VALID) dismiss()
            }
        }
        //avisar que hay que seleccionar un caballo
    }

    private fun setupRecyclerView() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        adapter = HorseItemAdapter(viewModel.data.value!!) { selectedItem: HorseItem ->
            selectedHorse = selectedItem
            binding.selectedItemImage.setImageURI(selectedItem.imageUri)
            binding.selectedItemText.text = selectedItem.name
            binding.selectedItemLayout.visibility = LinearLayout.VISIBLE

            toggleDropdown()
            setImageData(selectedItem)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun setImageData(selectedItem: HorseItem) {
        // Si se guardó la imagen localmente, agrego en la info el nombre del caballo
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        if(imagePath!!.isNotEmpty()) {
            ImageInfo.updateField(
                requireContext(),
                imagePath,
                "horseName",
                selectedItem.name
            )
        }
    }

    private fun toggleDropdown() {
        if (isDropdownVisible) {
            binding.recyclerView.visibility = RecyclerView.GONE
            binding.dropdownScroll.visibility = ScrollView.GONE

        } else {
            binding.recyclerView.visibility = RecyclerView.VISIBLE
            binding.dropdownScroll.visibility = ScrollView.VISIBLE
        }
        isDropdownVisible = !isDropdownVisible
    }

    private suspend fun uploadPhoto(
        prediction: String
    ): HorseCreatorActivity.ValidationState {
        return withContext(Dispatchers.IO) {
            try {
                val serenoValue = arguments?.getFloat(ARG_SERENO)
                val interesadoValue = arguments?.getFloat(ARG_INTERESADO)
                val disgustadoValue = arguments?.getFloat(ARG_DISGUSTADO)

                val jsonDtoAnalysis =
                    buildJson(prediction, serenoValue!!, interesadoValue!!, disgustadoValue!!)

                // Construir el cuerpo de la solicitud multipart
                val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                // Agregar la parte del JSON del caballo
                val jsonMediaType = "application/json".toMediaTypeOrNull()
                val analysisPart = jsonDtoAnalysis.toString().toRequestBody(jsonMediaType)
                multipartBuilder.addFormDataPart("analysis", "analysis.json", analysisPart)

                val image: ByteArray? = imageUri?.let {
                    imageHelper.getByteArrayImage(
                        requireContext(), it
                    )
                }
                // Agregar la parte de la imagen si existe
                image?.let {
                    val imageMediaType = "image/jpeg".toMediaTypeOrNull()
                    val imagePart = it.toRequestBody(imageMediaType)
                    multipartBuilder.addFormDataPart("image", "imagen.jpg", imagePart)
                }

                val multipartBody = multipartBuilder.build()
                val token = Network.getAccessToken()
                val request =
                    Request.Builder().url("${Network.BASE_URL}/api/analysis").post(multipartBody)
                        .addHeader("Authorization", "Bearer $token").build()

                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    HorseCreatorActivity.ValidationState.VALID
                } else {
                    HorseCreatorActivity.ValidationState.INVALID
                }
            } catch (e: IOException) {
                Log.d(TAG, e.stackTraceToString())
                e.printStackTrace()
                HorseCreatorActivity.ValidationState.INVALID
            }
        }
    }

    private fun buildJson(
        prediction: String, serenoValue: Float, interesadoValue: Float, disgustadoValue: Float
    ): JSONObject {
        val json = JSONObject()

        json.put("userId", Network.getIdUsuario())
        json.put("horseId", selectedHorse!!.id)
        json.put("prediction", prediction)
        json.put("sereno", serenoValue)
        json.put("interesado", interesadoValue)
        json.put("disgustado", disgustadoValue)

        return json
    }

    private fun showProgressDialog() {
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    private fun cancelProgressDialog() {
        customProgressDialog.dismiss()
    }

}