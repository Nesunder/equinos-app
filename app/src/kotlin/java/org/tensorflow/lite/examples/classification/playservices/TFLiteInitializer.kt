package org.tensorflow.lite.examples.classification.playservices

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite


class TFLiteInitializer private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: TFLiteInitializer? = null
        private val TAG = TFLiteInitializer::class.java.simpleName

        fun getInstance(): TFLiteInitializer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TFLiteInitializer().also {
                    INSTANCE = it
                }
            }
        }
    }

    private var useGpu = false
    private var isTFLiteInitialized = false

    fun initializeTask(context: Context): Task<Void> {
        return if (!isTFLiteInitialized) {
            TfLite.initialize(
                context,
                TfLiteInitializationOptions.builder().setEnableGpuDelegateSupport(true).build()
            ).continueWithTask { task ->
                if (task.isSuccessful) {
                    useGpu = true
                    isTFLiteInitialized = true
                    return@continueWithTask Tasks.forResult(null)
                } else {
                    // Fallback to initialize interpreter without GPU
                    isTFLiteInitialized = true
                    return@continueWithTask TfLite.initialize(context)
                }
            }.addOnFailureListener {
                Log.e(
                    TAG,
                    "TFLite in Play Services failed to initialize.",
                    it
                )
            }
        } else {
            Tasks.forResult(null)
        }
    }
}