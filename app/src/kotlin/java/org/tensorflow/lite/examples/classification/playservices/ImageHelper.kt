package org.tensorflow.lite.examples.classification.playservices

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

//no se si tiene mucho sentido el nombre pero bueno
class ImageHelper {
    private val isFrontFacing = false

    fun getBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        val imageBitmap: Bitmap? = getBitmapFromUri(uri, contentResolver)
        val inputStream = contentResolver.openInputStream(uri)

        //Obtengo la orientación de la imagen para dejarla derecha, así solo funciona desde api 24
        val exifInterface = inputStream?.let {
            ExifInterface(it)
        }

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )

        //Se aplican cambios en la imagen
        return imageBitmap?.let {
            rotateBitmap(
                it, orientation ?: ExifInterface.ORIENTATION_NORMAL
            )
        }
    }

    private fun getBitmapFromUri(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        return try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor != null) {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            if (isFrontFacing) postScale(-1f, 1f)
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    setRotate(180f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                else -> return bitmap
            }
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Create bitmap from view and returns it
     */
    fun getBitmapFromView(view: View): Bitmap {

        // Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.BLACK)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    suspend fun savePhotoFromBitmap(
        mBitmap: Bitmap?, contentResolver: ContentResolver
    ): String {
        return withContext(Dispatchers.IO) {
            mBitmap?.let {
                try {
                    val uri = getUriForFile(contentResolver)
                    uri?.let {
                        saveBitmapWithUri(it, mBitmap, contentResolver)
                        it.path!!
                    } ?: ""
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            } ?: ""
        }
    }

    private fun getUriForFile(contentResolver: ContentResolver): Uri? {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "EquinosApp_" + System.currentTimeMillis() / 1000
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/EquinosApp")
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + "/EquinosApp"
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdirs()
            }
            val imageFile = File(file, "EquinosApp_" + System.currentTimeMillis() / 1000 + ".jpg")
            Uri.fromFile(imageFile)
        }
    }

    private fun saveBitmapWithUri(uri: Uri, bitmap: Bitmap, contentResolver: ContentResolver) {
        contentResolver.openOutputStream(uri).use { outputStream ->
            outputStream?.let {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                it.flush()
                it.close()
            }
        }
    }

    // Función para obtener la uri de un bitmap, guardado temporalmente en un archivo. Es para poder pasar la foto al menú de subida.
    // No es conveniente pasar bitmaps entre acitividades/fragmentos porque pueden ocasionar problemas si pesan mucho
    suspend fun getImageUriFromBitmap(
        context: Context, bitmap: Bitmap
    ): Uri? {
        return withContext(Dispatchers.IO) {
            val tempFile =
                File(context.cacheDir, "temp_image_" + System.currentTimeMillis() / 1000 + ".jpg")
            try {
                FileOutputStream(tempFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                FileProvider.getUriForFile(
                    context, context.applicationContext.packageName + ".provider", tempFile
                )
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}