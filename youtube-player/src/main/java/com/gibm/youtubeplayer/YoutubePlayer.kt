package com.gibm.youtubeplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlin.math.abs

/**
 * Degradado bajo la barra de controles propia, a la manera del reproductor incrustado de
 * YouTube: transparente arriba, negro abajo, para que se lea sobre cualquier video.
 */
private val CONTROLS_GRADIENT = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
)
private val CONTROLS_GRADIENT_HEIGHT = 56.dp
private val CONTROLS_PADDING = 12.dp
private val CONTROLS_GAP = 8.dp
private val CONTROLS_ICON_BUTTON_SIZE = 32.dp
private val CONTROLS_ICON_SIZE = 20.dp

/**
 * Pista delgada a mano, no el `Slider` de Material: YouTube no expone miniaturas por segundo en
 * el IFrame Player API (son de un storyboard interno, sacarlas seria raspar algo no documentado),
 * asi que lo que si se puede calcar es el grosor de su barra y el globito de tiempo al arrastrar.
 */
private val SEEKBAR_TOUCH_HEIGHT = 24.dp
private val SEEKBAR_TRACK_HEIGHT = 2.dp
private val SEEKBAR_THUMB_SIZE = 10.dp
private val SEEKBAR_THUMB_SIZE_DRAGGING = 14.dp
private val SEEK_BUBBLE_WIDTH = 44.dp
private val SEEK_BUBBLE_HEIGHT = 24.dp
private val SEEK_BUBBLE_GAP = 8.dp
private val SEEK_BUBBLE_SHAPE = RoundedCornerShape(6.dp)

/**
 * Que tan cerca tiene que reportarse el reproductor del punto al que se arrastro para dejarlo
 * llevar la barra otra vez. Generoso a proposito: el aviso de `onCurrentSecond` no llega en cada
 * fotograma, y retroceder puede tardar un poco mas en asentarse que avanzar.
 */
private const val SEEK_SETTLE_TOLERANCE_SECONDS = 1.5f

/**
 * El reproductor embebido de un video de YouTube. Se apoya en android-youtube-player y no en un
 * `WebView` propio porque esa libreria ya resuelve la aceleracion por hardware dentro de un
 * `Dialog` y el pintado del video dentro de un recorte redondeado.
 *
 * La barra nativa del IFrame va apagada y en su lugar se pinta una propia en Compose.
 *
 * La pantalla completa la lleva quien lo llama: este composable solo avisa con
 * [onFullscreenChange]. Para que el video no se reinicie al cambiar de tamaño, el llamador lo
 * envuelve en `movableContentOf` y lo pinta en un lugar u otro segun [isFullscreen].
 *
 * @param videoId id del video (lo que va despues de `v=` en el enlace). Si cambia, carga el nuevo
 * en el mismo reproductor.
 * @param timeTextStyle estilo de los tiempos; el color lo pone [colors].
 */
@Composable
public fun YoutubePlayer(
    videoId: String,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    timeTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    colors: YoutubePlayerColors = YoutubePlayerColors(),
    labels: YoutubePlayerLabels = YoutubePlayerLabels(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var seekPosition by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(player, videoId) {
        player?.loadVideo(videoId, 0f)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val options = IFramePlayerOptions.Builder(context)
                    .controls(0)
                    .rel(0)
                    .ivLoadPolicy(3)
                    .ccLoadPolicy(0)
                    .build()

                YouTubePlayerView(context).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                player = youTubePlayer
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState,
                            ) {
                                isPlaying = state == PlayerConstants.PlayerState.PLAYING
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                val target = seekPosition
                                when {
                                    target == null -> currentSecond = second
                                    abs(second - target) < SEEK_SETTLE_TOLERANCE_SECONDS -> {
                                        seekPosition = null
                                        currentSecond = second
                                    }
                                }
                            }

                            override fun onVideoDuration(youTubePlayer: YouTubePlayer, videoDuration: Float) {
                                duration = videoDuration
                            }
                        },
                        options,
                    )
                }
            },
            onRelease = { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            },
        )

        YoutubeControls(
            isPlaying = isPlaying,
            currentSecond = seekPosition ?: currentSecond,
            duration = duration,
            isFullscreen = isFullscreen,
            onPlayPauseClick = { if (isPlaying) player?.pause() else player?.play() },
            onSeekChange = { seekPosition = it },
            onSeekFinished = { seekPosition?.let { player?.seekTo(it) } },
            onFullscreenClick = { onFullscreenChange(!isFullscreen) },
            timeTextStyle = timeTextStyle.copy(color = colors.content),
            colors = colors,
            labels = labels,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/** Barra de controles propia: play/pausa, progreso y pantalla completa. */
@Composable
private fun YoutubeControls(
    isPlaying: Boolean,
    currentSecond: Float,
    duration: Float,
    isFullscreen: Boolean,
    onPlayPauseClick: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onFullscreenClick: () -> Unit,
    timeTextStyle: TextStyle,
    colors: YoutubePlayerColors,
    labels: YoutubePlayerLabels,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(CONTROLS_GRADIENT_HEIGHT)
            .background(CONTROLS_GRADIENT)
            .padding(horizontal = CONTROLS_PADDING),
    ) {
        CompactIconButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) labels.pause else labels.play,
            tint = colors.content,
            onClick = onPlayPauseClick,
        )

        Text(text = formatSeconds(currentSecond), style = timeTextStyle)

        YoutubeSeekBar(
            currentSecond = currentSecond,
            duration = duration,
            onSeekChange = onSeekChange,
            onSeekFinished = onSeekFinished,
            timeTextStyle = timeTextStyle,
            colors = colors,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = CONTROLS_GAP),
        )

        Text(text = formatSeconds(duration), style = timeTextStyle)

        CompactIconButton(
            icon = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = if (isFullscreen) labels.exitFullscreen else labels.enterFullscreen,
            tint = colors.content,
            onClick = onFullscreenClick,
        )
    }
}

/** Un boton de icono mas chico que el de Material, a la densidad de la barra de YouTube. */
@Composable
private fun CompactIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(CONTROLS_ICON_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(CONTROLS_ICON_SIZE),
        )
    }
}

/** Pista de progreso a la manera de YouTube, con el globito de tiempo mientras se arrastra. */
@Composable
private fun YoutubeSeekBar(
    currentSecond: Float,
    duration: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    timeTextStyle: TextStyle,
    colors: YoutubePlayerColors,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val fraction = if (duration > 0f) (currentSecond / duration).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .height(SEEKBAR_TOUCH_HEIGHT)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(duration) {
                if (duration <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished()
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onSeekChange(newFraction * duration)
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SEEKBAR_TRACK_HEIGHT)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(colors.content.copy(alpha = 0.3f)),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(SEEKBAR_TRACK_HEIGHT)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(colors.progress),
        )

        val thumbSize = if (isDragging) SEEKBAR_THUMB_SIZE_DRAGGING else SEEKBAR_THUMB_SIZE
        val thumbOffset = with(density) { (widthPx * fraction).toDp() } - thumbSize / 2
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(colors.progress),
        )

        if (isDragging) {
            val bubbleOffset = with(density) { (widthPx * fraction).toDp() } - SEEK_BUBBLE_WIDTH / 2
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = bubbleOffset, y = -(SEEK_BUBBLE_HEIGHT + SEEK_BUBBLE_GAP))
                    .size(width = SEEK_BUBBLE_WIDTH, height = SEEK_BUBBLE_HEIGHT)
                    .clip(SEEK_BUBBLE_SHAPE)
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = formatSeconds(currentSecond), style = timeTextStyle)
            }
        }
    }
}

/** El tiempo de un video en "m:ss", para la barra de progreso. */
internal fun formatSeconds(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** La barra de controles sola: el video es un `WebView` y el preview no lo puede pintar. */
@Preview(widthDp = 360, backgroundColor = 0xFF333333, showBackground = true)
@Composable
private fun YoutubeControlsPreview() {
    YoutubeControls(
        isPlaying = true,
        currentSecond = 74f,
        duration = 312f,
        isFullscreen = false,
        onPlayPauseClick = {},
        onSeekChange = {},
        onSeekFinished = {},
        onFullscreenClick = {},
        timeTextStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
        colors = YoutubePlayerColors(),
        labels = YoutubePlayerLabels(),
    )
}
