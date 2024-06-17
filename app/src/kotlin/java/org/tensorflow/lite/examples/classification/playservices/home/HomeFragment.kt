package org.tensorflow.lite.examples.classification.playservices.home

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.classification.playservices.ImageClassificationActivity
import org.tensorflow.lite.examples.classification.playservices.databinding.FragmentHomeBinding
import org.tensorflow.lite.examples.classification.playservices.horseCreation.HorseCreatorActivity


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val pickIntent by lazy {
        Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null && result.data?.data != null) {
                val uri = result.data?.data!!
                val openCameraIntent =
                    Intent(activity, ImageClassificationActivity::class.java).putExtra(
                        "imageUri",
                        uri
                    )
                startActivity(openCameraIntent)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.classifyImageBtn.setOnClickListener {
            openGallery()
        }

        val horseCreatorActivity = Intent(context, HorseCreatorActivity::class.java)

        binding.addHorseBtn.setOnClickListener {
            startActivity(horseCreatorActivity)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openGallery() {
        openGalleryLauncher.launch(pickIntent)
    }
}