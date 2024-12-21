package com.equinos.horseCreation

import android.net.Uri

data class HorseItem(
    val id: Long,
    val name: String,
    val imageUri: Uri
)