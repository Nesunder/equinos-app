package org.tensorflow.lite.examples.classification.playservices.gallery

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.classification.playservices.databinding.FragmentGalleryBinding
import java.io.File


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private var imageList = ArrayList<Image>()
    private val imageViewerIntent by lazy { Intent(activity, ImageViewerActivity::class.java) }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val filePath = "/storage/emulated/0/DCIM/EquinosApp"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.setHasFixedSize(true)

        getImages()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        getImages()
    }

    private fun getImages() {
        imageList.clear()
        val file = File(filePath)
        scanDirectory()
        val files = file.listFiles()
        if (files != null) {
            for (fileItem in files) {
                if (fileItem.path.endsWith(".png") || fileItem.path.endsWith(".jpg")) {
                    imageList.add(
                        Image(
                            fileItem.getName(),
                            fileItem.path,
                            fileItem.length(),
                            "Sereno",
                            "Tiro al blanco",
                            1,
                            "pepe"
                        )
                    )
                }
            }
        }
        val adapter = activity?.let { ImageAdapter(it, imageList) }
        binding.recyclerView.setAdapter(adapter)
        adapter?.setOnItemClickListener { _: View?, path: String? ->
            startActivity(
                imageViewerIntent.putExtra("image", path)
            )
        }
    }

    // Escaneo del directorio donde se guardan las imágenes para que no se muestren las que se eliminaron desde la galería
    private fun scanDirectory() {
        val directory = File(filePath)
        if (directory.exists() && directory.isDirectory) {
            MediaScannerConnection.scanFile(
                requireContext(),
                arrayOf(directory.toString()),
                null,
                null
            )
        }
    }
}
