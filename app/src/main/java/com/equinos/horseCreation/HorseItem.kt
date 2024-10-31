package com.equinos.horseCreation

import android.net.Uri

data class HorseItem(
    var id: Long,
    var name: String,
    var imageUri: Uri
)