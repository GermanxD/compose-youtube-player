# compose-youtube-player

Reproductor de YouTube para Jetpack Compose con una barra de controles propia (play/pausa,
progreso con globito de tiempo y pantalla completa). Por dentro usa
[android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) con la
barra nativa del IFrame apagada.

## Instalar

Requiere minSdk 23.

Se publica con [JitPack](https://jitpack.io/#GermanxD/compose-youtube-player) desde los tags del
repo; no hace falta token. En el `settings.gradle.kts` de la app:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content { includeGroup("com.github.GermanxD") }
        }
    }
}
```

Y en el módulo:

```kotlin
implementation("com.github.GermanxD:compose-youtube-player:v1.0.1")
```

## Uso

La pantalla completa la maneja quien llama al reproductor. Para que el video no se reinicie al
cambiar de tamaño, envuélvelo en `movableContentOf` y píntalo donde toque:

```kotlin
var isFullscreen by remember { mutableStateOf(false) }
BackHandler(enabled = isFullscreen) { isFullscreen = false }

val player = remember {
    movableContentOf<Modifier> { modifier ->
        YoutubePlayer(
            videoId = "dQw4w9WgXcQ",
            isFullscreen = isFullscreen,
            onFullscreenChange = { isFullscreen = it },
            modifier = modifier,
        )
    }
}

if (isFullscreen) {
    player(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding())
} else {
    player(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
}
```

Se puede ajustar con:

- `timeTextStyle`: tipografía de los tiempos (por defecto `MaterialTheme.typography.labelSmall`).
- `colors = YoutubePlayerColors(progress = ..., content = ...)`.
- `labels = YoutubePlayerLabels(...)`: textos de accesibilidad de los botones (en español por defecto).

## Ejemplo

El módulo [`:sample`](sample/src/main/java/com/gibm/youtubeplayer/sample/MainActivity.kt) trae una
app completa con cambio de video, pantalla completa con `movableContentOf` y personalización de
colores. Para correrla en un dispositivo o emulador conectado:

```bash
./gradlew :sample:installDebug
```

O abre el repositorio en Android Studio y ejecuta la configuración **sample**.
