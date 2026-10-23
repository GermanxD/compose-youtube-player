package com.gibm.youtubeplayer.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gibm.youtubeplayer.YoutubePlayer
import com.gibm.youtubeplayer.YoutubePlayerColors

private data class SampleVideo(
    val id: String,
    val title: String,
    val subtitle: String,
)

private val SAMPLE_VIDEOS = listOf(
    SampleVideo(
        id = "aqz-KE-bpKQ",
        title = "Big Buck Bunny",
        subtitle = "Blender Foundation · 60fps",
    ),
    SampleVideo(
        id = "eRsGyueVLvQ",
        title = "Sintel",
        subtitle = "Blender Open Movie",
    ),
    SampleVideo(
        id = "dQw4w9WgXcQ",
        title = "Rick Astley - Never Gonna Give You Up",
        subtitle = "Video oficial",
    ),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                YoutubePlayerSampleScreen()
            }
        }
    }
}

@Composable
private fun YoutubePlayerSampleScreen() {
    var selectedVideo by remember { mutableStateOf(SAMPLE_VIDEOS.first()) }
    var isFullscreen by remember { mutableStateOf(false) }
    var useCustomColor by remember { mutableStateOf(false) }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val playerColors = if (useCustomColor) {
        YoutubePlayerColors(progress = MaterialTheme.colorScheme.primary)
    } else {
        YoutubePlayerColors()
    }

    val player = remember {
        movableContentOf<Modifier> { modifier ->
            YoutubePlayer(
                videoId = selectedVideo.id,
                isFullscreen = isFullscreen,
                onFullscreenChange = { isFullscreen = it },
                colors = playerColors,
                modifier = modifier,
            )
        }
    }

    if (isFullscreen) {
        player(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding(),
        )
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    text = "compose-youtube-player",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(12.dp))

                player(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = !useCustomColor,
                        onClick = { useCustomColor = false },
                        label = { Text("Rojo YouTube") },
                    )
                    FilterChip(
                        selected = useCustomColor,
                        onClick = { useCustomColor = true },
                        label = { Text("Color del tema") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Videos de prueba",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SAMPLE_VIDEOS.forEach { video ->
                        VideoOptionRow(
                            video = video,
                            selected = video == selectedVideo,
                            onClick = { selectedVideo = video },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoOptionRow(
    video: SampleVideo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${video.subtitle} (${video.id})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
