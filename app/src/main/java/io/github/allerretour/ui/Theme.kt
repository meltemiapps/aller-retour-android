package io.github.allerretour.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Colours from Προσευχητάριο, as on the iPhone app: Greek blue on sky glass by day, deep navy at night. */
object Brand {
    val AccentLight = Color(0xFF0D5EAF)
    val AccentDark = Color(0xFF6CB6FF)
    val Danger = Color(0xFFC62828)
    val Good = Color(0xFF2E9E5B)
    val Warn = Color(0xFFE08A00)
    val Navy = Color(0xFF0A1E38)

    fun accent(dark: Boolean) = if (dark) AccentDark else AccentLight

    fun hero(dark: Boolean): Brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFF1A5591), Color(0xFF0F3B6B), Color(0xFF082547))
        else listOf(Color(0xFF5AAEF0), Color(0xFF1E74C8), Color(0xFF0D5EAF)),
        start = Offset.Zero, end = Offset.Infinite,
    )
}

val LocalDark = staticCompositionLocalOf { false }

private val BrandLight = lightColorScheme(
    primary = Brand.AccentLight, onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE5FF), onPrimaryContainer = Color(0xFF0D3A6B),
    secondary = Color(0xFF1F6FC1), onSecondary = Color.White,
    background = Color(0xFFEEF6FF), onBackground = Color(0xFF0E2236),
    surface = Color(0xFFF7FAFF), onSurface = Color(0xFF0E2236),
    surfaceVariant = Color(0xFFE3EEFB), onSurfaceVariant = Color(0xFF4A5F78),
    surfaceContainerHigh = Color.White, surfaceContainerLow = Color(0xFFF2F7FD),
    outline = Color(0xFFB9CBE0), error = Brand.Danger,
)

private val BrandDark = darkColorScheme(
    primary = Brand.AccentDark, onPrimary = Color(0xFF04213F),
    primaryContainer = Color(0xFF12406F), onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF8CC6FF), onSecondary = Color(0xFF04213F),
    background = Color(0xFF07182C), onBackground = Color(0xFFEAF3FF),
    surface = Color(0xFF0C2340), onSurface = Color(0xFFEAF3FF),
    surfaceVariant = Color(0xFF14304F), onSurfaceVariant = Color(0xFF9DB4CE),
    surfaceContainerHigh = Color(0xFF12304F), surfaceContainerLow = Color(0xFF0A1E38),
    outline = Color(0xFF2F4F72), error = Color(0xFFFF6B6B),
)

/** Always the brand palette, in light or dark to follow the phone. Never the wallpaper colours. */
@Composable
fun AllerRetourTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) BrandDark else BrandLight, content = content)
    }
}
