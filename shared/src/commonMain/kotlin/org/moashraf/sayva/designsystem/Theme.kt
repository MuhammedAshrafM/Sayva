package org.moashraf.sayva.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import sayva.shared.generated.designsystem.sayvaTypography

private val SayvaColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Surface,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary50,
    onSecondary = Surface,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary50,
    onTertiary = Surface,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorColor,
    onError = Surface,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorColor,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineStrong,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerLowest = Surface,
    surfaceDim = SurfaceDim,
)

@Composable
fun SayvaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SayvaColorScheme,
        typography = sayvaTypography(),
        shapes = SayvaShapes,
        content = content,
    )
}
