package com.equinos.gallery

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.equinos.databinding.FragmentGalleryBinding
import java.io.File


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private var imageList = ArrayList<ImageInfo>()
    private val imageViewerIntent by lazy {
        Intent(
            requireActivity(),
            ImageViewerActivity::class.java
        )
    }

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private val filePath = "/storage/emulated/0/DCIM/EquinosApp"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.recyclerView.setHasFixedSize(true)
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            if (isAdded) {
                getImages()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun getImages() {
        withContext(Dispatchers.IO) {
            imageList.clear()
            val file = File(filePath)
            scanDirectory()
            val files = file.listFiles()
            files?.filter { it.path.endsWith(".png") || it.path.endsWith(".jpg") }?.forEach {
                val imageInfo = ImageInfo.load(requireContext(), it.path)
                imageList.add(
                    ImageInfo(
                        it.name,
                        it.path,
                        it.length(),
                        imageInfo?.prediction ?: "",
                        imageInfo?.horseName ?: "",
                        1,
                        "pepe"
                    )
                )
            }
        }
        if (isAdded) {
            val adapter = ImageAdapter(requireContext(), imageList)
            binding.recyclerView.adapter = adapter
            adapter.setOnItemClickListener { _, path ->
                startActivity(
                    imageViewerIntent.putExtra("image", path)
                )
            }
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