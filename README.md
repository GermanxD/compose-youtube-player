# compose-youtube-player

Reproductor de YouTube para Jetpack Compose con una barra de controles propia (play/pausa,
progreso con globito de tiempo y pantalla completa). Por dentro usa
[android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) con la
barra nativa del IFrame apagada.

Salió de TigreFit (`:core:designsystem`). minSdk 23.

## Instalar

Se publica en GitHub Packages, que pide un token aunque solo vayas a leer. Crea un token clásico
con `read:packages` y ponlo en `~/.gradle/gradle.properties`:

```properties
gpr.user=tu-usuario-de-github
gpr.key=ghp_...
```

En el `settings.gradle.kts` de la app:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.github.com/GermanxD/compose-youtube-player") {
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
            content { includeGroup("com.gibm") }
        }
    }
}
```

Y en el módulo:

```kotlin
implementation("com.gibm:youtube-player:1.0.0")
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
    player(Modifier.fillMaxSize().background(Color.Black).safeAreaPadding())
} else {
    player(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
}
```

Se puede ajustar con:

- `timeTextStyle`: tipografía de los tiempos (por defecto `MaterialTheme.typography.labelSmall`).
- `colors = YoutubePlayerColors(progress = ..., content = ...)`.
- `labels = YoutubePlayerLabels(...)`: textos de accesibilidad de los botones (en español por defecto).

## Probar un cambio sin publicar

```bash
./gradlew publishToMavenLocal
```

y en la app pon `mavenLocal()` antes del repositorio de GitHub Packages.

## Publicar una versión

```bash
git tag v1.0.1 && git push origin v1.0.1
```

`.github/workflows/publish.yml` publica con la versión del tag (sin la `v`).
