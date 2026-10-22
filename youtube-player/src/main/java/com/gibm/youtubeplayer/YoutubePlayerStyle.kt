package com.gibm.youtubeplayer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colores de la barra de controles propia.
 *
 * @property progress la parte recorrida de la pista y su perilla; por defecto el rojo de YouTube.
 * @property content iconos y tiempos, que van sobre el degradado oscuro.
 */
@Immutable
public data class YoutubePlayerColors(
    val progress: Color = Color(0xFFFF0000),
    val content: Color = Color.White,
)

/**
 * Textos de accesibilidad de los botones. Vienen en español porque las apps que lo usan son en
 * español; una app en otro idioma pasa los suyos, normalmente con `stringResource`.
 */
@Immutable
public data class YoutubePlayerLabels(
    val play: String = "Reproducir",
    val pause: String = "Pausar",
    val enterFullscreen: String = "Pantalla completa",
    val exitFullscreen: String = "Salir de pantalla completa",
)
