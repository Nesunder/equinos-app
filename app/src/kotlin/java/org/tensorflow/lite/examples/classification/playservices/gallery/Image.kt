package org.tensorflow.lite.examples.classification.playservices.gallery

data class Image(
    val title: String,
    val path: String,
    val size: Long,
    val classification: String,
    val horseName: String,
    val userId: Long,
    val user: String
)
