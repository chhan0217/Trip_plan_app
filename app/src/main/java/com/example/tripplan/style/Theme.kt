package com.example.tripplan.style

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

internal val LightColorScheme = lightColorScheme(
    primary = ColorStyles.keyColor01,
    secondary = ColorStyles.keyColor01Dark01,
    tertiary = ColorStyles.keyColor01Dark02,

    background = ColorStyles.white,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun CVTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = 1f
        )
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            content = content
        )
    }
}

@Composable
fun TripPlanTheme(
    content: @Composable () -> Unit
) {
    CVTheme(content = content)
}
