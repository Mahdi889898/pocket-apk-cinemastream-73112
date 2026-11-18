package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CinemaDarkColorScheme = darkColorScheme(
    primary = CinemaGold,
    onPrimary = ObsidianBg,
    primaryContainer = CinemaGoldContainer,
    onPrimaryContainer = CinemaGold,
    secondary = StreamCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = StreamCyanContainer,
    onSecondaryContainer = StreamCyan,
    tertiary = LiveCrimson,
    onTertiary = TextPrimary,
    tertiaryContainer = LiveCrimsonContainer,
    onTertiaryContainer = LiveCrimson,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    outlineVariant = TextMuted,
    error = LiveCrimson,
    onError = TextPrimary,
)

private val CinemaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CinemaStreamTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinemaDarkColorScheme,
        typography = AppTypography,
        shapes = CinemaShapes,
        content = content,
    )
}
