package org.tensorflow.lite.examples.classification.playservices.horseCreation

import android.net.Uri

data class HorseItem(
    var id: Long,
    var name: String,
    var imageUri: Uri
)