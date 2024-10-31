package com.equinos.tips

import com.equinos.R

object TipsList {
    val tipsList: List<TipsDomain> = listOf(
        TipsDomain("Cómo clasificar una foto", "Para clasificar una foto...", R.drawable.tips_foto),
        TipsDomain(
            "Sacar una foto y clasificar", "Para sacar una foto...", R.drawable.tips_capture
        ),
        TipsDomain(
            "Clasificar foto de la galería, desde la cámara",
            "Para clasificar una foto...",
            R.drawable.tips_capture_galery
        ),
        TipsDomain(
            "Interfaz de clasificación dedicada",
            "Para clasificar una foto ya capturada...",
            R.drawable.tips_dedicada
        )
    )
}