package com.equinos.gallery

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat.IntentBuilder
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.equinos.R
import com.equinos.databinding.ActivityImageViewerBinding
import java.io.File


class ImageViewerActivity : AppCompatActivity() {
    private lateinit var imageViewerBinding: ActivityImageViewerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        imageViewerBinding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(imageViewerBinding.root)

        var path: String? = null
        val intent = intent

        if (intent != null) {
            Glide.with(this@ImageViewerActivity).load(intent.getStringExtra("image"))
                .placeholder(R.drawable.ic_baseline_broken_image_24)
                .into(imageViewerBinding.imageView)
            path = intent.getStringExtra("image")
        }

        val finalPath = path
        imageViewerBinding.shareImage.setOnClickListener {
            IntentBuilder(this@ImageViewerActivity).setStream(
                Uri.parse(finalPath)
            ).setType("image/*").setChooserTitle("Compartir Imagen").startChooser()
        }

        imageViewerBinding.deleteImage.setOnClickListener {
            finalPath?.let { path -> deleteImage(path) }
        }

        onBackPressedDispatcher.addCallback {
            finish()
        }

    }

    private fun deleteImage(finalPath: String) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this@ImageViewerActivity)
        alertDialogBuilder.setMessage("Está seguro que desa eliminar esta imagen?")
        alertDialogBuilder.setPositiveButton(
            "Si"
        ) { _, _ ->
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = "${MediaStore.Images.Media.DATA} = ?"
            val selectionArgs = arrayOf(File(finalPath).absolutePath)
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver = contentResolver
            val cursor =
                contentResolver.query(queryUri, projection, selection, selectionArgs, null)

            if (cursor!!.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val deleteUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                try {
                    contentResolver.delete(deleteUri, null, null)
                    File(finalPath).delete()
                    Toast.makeText(
                        this@ImageViewerActivity, "Imagen eliminada correctamente", Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@ImageViewerActivity, "Error eliminando la imagen", Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@ImageViewerActivity, "No se encontró el archivo", Toast.LENGTH_SHORT
                ).show()
            }
            cursor.close()
        }
        alertDialogBuilder.setNegativeButton(
            "No"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialogBuilder.show()
    }
}