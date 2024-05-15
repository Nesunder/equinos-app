package org.tensorflow.lite.examples.classification.playservices

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

//no se si tiene mucho sentido el nombre pero bueno
class ImageHelper {

    private val isFrontFacing = false

    fun getBitmap(uri: Uri, contentResolver: ContentResolver): Bitmap? {
        val imageBitmap: Bitmap? = getBitmapFromUri(uri, contentResolver)
        val inputStream = contentResolver.openInputStream(uri)

        //Obtengo la orientación de la imagen para dejarla derecha, así solo funciona desde api 24
        val exifInterface = inputStream?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface(it)
            } else {
                TODO("VERSION.SDK_INT < N")
            }
        }

        val orientation = exifInterface?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )

        //Se aplican cambios en la imagen
        val rotatedBitmap = imageBitmap?.let {
            configBitmap(
                it, orientation ?: ExifInterface.ORIENTATION_NORMAL
            )
        }
        return rotatedBitmap
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

    private fun configBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            if (isFrontFacing) postScale(-1f, 1f)
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> this.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> this.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    setRotate(180f)
                    this.postScale(-1f, 1f)
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
            val oriented =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            oriented
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

    suspend fun saveBitmapFile(
        mBitmap: Bitmap?,
        contentResolver: ContentResolver
    ): String {
        var path = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "EquinosApp_" + System.currentTimeMillis() / 1000
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/EquinosApp")
                    }
                    val uri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                    )

                    path = uri?.path!!

                    contentResolver.openOutputStream(uri).use { outputStream ->
                        // Use the outputStream to save your bitmap
                        if (outputStream != null) {
                            mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        }
                    }
                } catch (e: Exception) {
                    path = ""
                    e.printStackTrace()
                }
            }
        }
        return path
    }
}