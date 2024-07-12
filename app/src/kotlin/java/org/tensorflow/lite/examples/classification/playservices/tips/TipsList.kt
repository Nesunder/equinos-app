package org.tensorflow.lite.examples.classification.playservices.tips

import org.tensorflow.lite.examples.classification.playservices.R

object TipsList {
    val tipsList: List<TipsDomain> = listOf(
        TipsDomain("Cómo clasificar una foto", "Para clasificar una foto...", R.drawable.tips_foto),
        TipsDomain(
            "Sacar una foto y clasificar", "Para clasificar una foto...", R.drawable.tips_foto
        ),
        TipsDomain(
            "Clasificar foto de la galería, desde la cámara",
            "Para clasificar una foto...",
            R.drawable.tips_foto
        ),
        TipsDomain(
            "Clasificar foto de la galería, desde la cámara",
            "Para clasificar una foto...",
            R.drawable.tips_foto
        )
    )
}