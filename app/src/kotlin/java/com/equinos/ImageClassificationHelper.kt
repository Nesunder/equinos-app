/*
 * Copyright 2022 The TensorFlow Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.equinos

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.Closeable
import java.util.PriorityQueue
import kotlin.math.min
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options.TfLiteRuntime
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Helper class used to communicate between our app and the TF image classification model */
class ImageClassificationHelper(
    private val context: Context,
    private val maxResult: Int,
    private val useGpu: Boolean
) : Closeable {

    /** Abstraction object that wraps a classification output in an easy to parse way */
    data class Recognition(val id: String, val title: String, val confidence: Float)

    private val preprocessNormalizeOp = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    private val postprocessNormalizeOp = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)
    private val labels by lazy { FileUtil.loadLabels(context, LABELS_PATH) }
    private var tfInputBuffer = TensorImage(DataType.UINT8)
    private var tfImageProcessor: ImageProcessor? = null

    // Processor to apply post processing of the output probability
    private val probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()

    // Use TFLite in Play Services runtime by setting the option to FROM_SYSTEM_ONLY
    private val interpreterInitializer = lazy {
        val interpreterOption = InterpreterApi.Options()
            .setRuntime(TfLiteRuntime.FROM_SYSTEM_ONLY)

        if (useGpu) {
            interpreterOption.addDelegateFactory(GpuDelegateFactory())
        }

        InterpreterApi.create(FileUtil.loadMappedFile(context, MODEL_PATH), interpreterOption)

    }

    // Only use interpreter after initialization finished in CameraActivity
    private val interpreter: InterpreterApi by interpreterInitializer
    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = interpreter.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    // Output probability TensorBuffer
    private val outputProbabilityBuffer: TensorBuffer by lazy {
        val probabilityTensorIndex = 0
        val probabilityShape =
            interpreter.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
        val probabilityDataType = interpreter.getOutputTensor(probabilityTensorIndex).dataType()
        TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
    }

    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }

    /** Classifies the input bitmapBuffer. */
    //Metodo original con inferencia y procesamiento sacado de la app de img classification de tensorflow
    fun classify(bitmapBuffer: Bitmap, imageRotationDegrees: Int): List<Recognition> {
        // Loads the input bitmapBuffer
        tfInputBuffer = loadImage(bitmapBuffer, imageRotationDegrees)
        Log.d(TAG, "tensorSize: ${tfInputBuffer.width} x ${tfInputBuffer.height}")

        // Runs the inference call
        interpreter.run(tfInputBuffer.buffer, outputProbabilityBuffer.buffer.rewind())

        // Gets the map of label and probability
        val labeledProbability =
            TensorLabel(
                labels,
                probabilityProcessor.process(outputProbabilityBuffer)
            ).mapWithFloatValue

        return getProbabilities(labeledProbability)
    }

    /** Releases TFLite resources if initialized. */
    override fun close() {
        if (interpreterInitializer.isInitialized()) {
            interpreter.close()
        }
        imageClassifier?.close()
    }

    /** Loads input image, and applies preprocessing. */
    private fun loadImage(bitmapBuffer: Bitmap, imageRotationDegrees: Int): TensorImage {
        // Initializes preprocessor if null
        return (tfImageProcessor
            ?: run {
                val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
                ImageProcessor.Builder()
                    .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                    .add(
                        ResizeOp(
                            tfInputSize.height,
                            tfInputSize.width,
                            ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                        )
                    )
                    .add(Rot90Op(-imageRotationDegrees / 90))
                    .add(preprocessNormalizeOp)
                    .build()
                    .also {
                        tfImageProcessor = it
                        Log.d(
                            TAG,
                            "tfImageProcessor initialized successfully. imageSize: $cropSize"
                        )
                    }
            })
            .process(tfInputBuffer.apply { load(bitmapBuffer) })
    }

    /** Gets the top-k results. */
    private fun getProbabilities(labelProb: Map<String, Float>): List<Recognition> {
        // Sort the recognition by confidence from high to low.
        val pq: PriorityQueue<Recognition> =
            PriorityQueue(maxResult, compareByDescending<Recognition> { it.confidence })
        pq += labelProb.map { (label, prob) -> Recognition(label, label, prob) }
        return List(min(maxResult, pq.size)) { pq.poll()!! }
    }

    //Clasificador alternativo
    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setMaxResults(maxResult)

        val numberOfCores = Runtime.getRuntime().availableProcessors()

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numberOfCores - 2)

        if (useGpu && CompatibilityList().isDelegateSupportedOnThisDevice) {
            baseOptionsBuilder.useGpu()
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(
                    context,
                    MODEL_PATH,
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    //Metodo alternativo
    fun classifyWithMetadata(image: Bitmap, rotation: Int): List<Category>? {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        val imageProcessor =
            ImageProcessor.Builder()
                .add(ResizeOp(299, 299, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0.0f, 1.0f)) // Normalize pixel values to [0, 1]
                .build()
        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
        Log.d(TAG, "tensorSize: ${tensorImage.width} x ${tensorImage.height}")

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        return updateResults(results)
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    private fun updateResults(listClassifications: List<Classifications>?): List<Category>? {
        var sortedCategories: List<Category>? = null
        listClassifications?.let { it ->
            if (it.isNotEmpty()) {
                sortedCategories = it[0].categories.sortedBy { it?.index }
            }
        }
        return sortedCategories
    }

    //Procesamiento manual, metodo actual
    fun classifyImageManualProcessing(bitmap: Bitmap): List<Recognition> {
        // Preprocess the image
        val inputBuffer = preprocessBitmap(bitmap)

        // Run the model
        interpreter.run(inputBuffer.buffer, outputProbabilityBuffer.buffer.rewind())

        val labeledProbability =
            TensorLabel(
                labels,
                probabilityProcessor.process(outputProbabilityBuffer)
            ).mapWithFloatValue

        return getProbabilities(labeledProbability)
    }

    private fun preprocessBitmap(bitmap: Bitmap): TensorBuffer {
        // Resize the bitmap to the expected width and height
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap, EXPECTED_WIDTH, EXPECTED_HEIGHT, true
        )

        // Convert the bitmap to a ByteBuffer with normalized pixel values
        val inputSize = EXPECTED_WIDTH * EXPECTED_HEIGHT * COLORS
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(EXPECTED_WIDTH * EXPECTED_HEIGHT)
        resizedBitmap.getPixels(intValues, 0, 299, 0, 0, EXPECTED_WIDTH, EXPECTED_HEIGHT)

        for (pixelValue in intValues) {
            // Normalize pixel values to [0, 1] range
            byteBuffer.putFloat(((pixelValue shr 16 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixelValue shr 8 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixelValue and 0xFF) / 255.0f))
        }

        // Create a TensorBuffer from the normalized image data
        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, EXPECTED_WIDTH, EXPECTED_HEIGHT, COLORS),
            DataType.FLOAT32
        )
        inputBuffer.loadBuffer(byteBuffer)

        return inputBuffer
    }

    fun filterRecognitionsByTitle(
        recognitions: List<Recognition>, title: String
    ): List<Recognition> {
        return recognitions.filter { it.title == title }
    }

    companion object {
        private val TAG = ImageClassificationHelper::class.java.simpleName

        // ClassifierFloatEfficientNet model
        private const val MODEL_PATH = "equinosMetadata-v3.tflite"
        private const val LABELS_PATH = "labels.txt"

        // Float model does not need dequantization in the post-processing. Setting mean and std as
        // 0.0f and 1.0f, respectively, to bypass the normalization
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 1.0f
        private const val IMAGE_MEAN = 127.0f
        private const val IMAGE_STD = 128.0f
        private const val EXPECTED_HEIGHT = 299
        private const val EXPECTED_WIDTH = 299
        private const val COLORS = 3
    }
}
