package com.proyecto.ganapp.ui.common.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema claro GANAPP v1.
 *
 * Los roles de Material 3 sin token propio reutilizan colores aprobados para no
 * conservar el púrpura/rosa del template ni inventar hex nuevos:
 * - inversePrimary = PrimaryContainer
 * - onSecondary, onTertiary y onError = OnPrimary
 * - secondaryContainer, tertiaryContainer, surfaceContainer y surfaceContainerHigh = SurfaceVariant
 * - onSecondaryContainer, onTertiaryContainer y onBackground = OnSurface
 * - tertiary = Secondary (no hay un tercer acento aprobado)
 * - inverseSurface = OnSurface, inverseOnSurface = Surface
 * - outlineVariant, surfaceContainerHighest y surfaceDim = DisabledContainer
 * - surfaceBright y surfaceContainerLowest = Surface
 * - surfaceContainerLow = Background
 * - surfaceTint = Primary
 *
 * errorContainer, onErrorContainer y scrim conservan el default de Material 3
 * (contenedor de error e ink de error, y negro de scrim). No son el template púrpura
 * y no había token GANAPP aprobado para ellos.
 *
 * No existe darkColorScheme. El modo oscuro se añadirá aquí cuando haya paleta aprobada.
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = PrimaryContainer,
    secondary = Secondary,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceVariant,
    onSecondaryContainer = OnSurface,
    tertiary = Secondary,
    onTertiary = OnPrimary,
    tertiaryContainer = SurfaceVariant,
    onTertiaryContainer = OnSurface,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Primary,
    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    error = Error,
    onError = OnPrimary,
    outline = Outline,
    outlineVariant = DisabledContainer,
    surfaceBright = Surface,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Background,
    surfaceContainer = SurfaceVariant,
    surfaceContainerHigh = SurfaceVariant,
    surfaceContainerHighest = DisabledContainer,
    surfaceDim = DisabledContainer
)

/**
 * Theme GANAPP v1. Solo light.
 *
 * No consulta el tema del sistema. [dynamicColor] queda en false: la paleta aprobada
 * no depende del wallpaper. Si un caller lo activa, solo se usa el esquema dinámico claro.
 */
@Composable
fun GanAppTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GanAppShapes,
        content = content
    )
}
