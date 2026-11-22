package io.github.pocketbook.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Teal = Color(0xFF0D5EAF)
val TealLight = Color(0xFFCFE5FF)
val Coral = Color(0xFFE2603F)
val Paper = Color(0xFFEEF6FF)
val Ink = Color(0xFF0E2236)
val Good = Color(0xFF2E8B57)
val GoodBg = Color(0xFFE2F3E8)
val WarnFg = Color(0xFF8A5D00)
val WarnBg = Color(0xFFFDF0D8)
val BadFg = Color(0xFFA8391D)
val BadBg = Color(0xFFFBE3DC)

// Brand palette (shared with Προσευχητάριο): Greek blue on sky, deep navy at night.
// Used on Android 11 and older, or when the user turns off wallpaper colours.
private val BrandLight = lightColorScheme(
    primary = Color(0xFF0D5EAF), onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE5FF), onPrimaryContainer = Color(0xFF0D3A6B),
    secondary = Color(0xFF1F6FC1), onSecondary = Color.White,
    tertiary = Color(0xFF8C6A22), tertiaryContainer = Color(0xFFF6E7C1), onTertiaryContainer = Color(0xFF5A4410),
    background = Color(0xFFEEF6FF), onBackground = Color(0xFF0E2236),
    surface = Color.White, onSurface = Color(0xFF0E2236),
    surfaceVariant = Color(0xFFE3EEFB), onSurfaceVariant = Color(0xFF4F6784),
    outline = Color(0xFFCFDDED),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFF6CB6FF), onPrimary = Color(0xFF04213F),
    primaryContainer = Color(0xFF12406F), onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF8CC6FF), onSecondary = Color(0xFF04213F),
    tertiary = Color(0xFFE0C27A), tertiaryContainer = Color(0xFF4A3A14), onTertiaryContainer = Color(0xFFF3E2B0),
    background = Color(0xFF07182C), onBackground = Color(0xFFEAF3FF),
    surface = Color(0xFF0C2340), onSurface = Color(0xFFEAF3FF),
    surfaceVariant = Color(0xFF14304F), onSurfaceVariant = Color(0xFF8FA9C6),
    outline = Color(0xFF23415F),
)

/** Wallpaper (dynamic) colours need Android 12, API 31. */
val supportsWallpaperColours: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Default: the phone's wallpaper colours (Android 12 and later).
 * Falls back to the brand palette on older phones, or when [useWallpaperColours] is off.
 */
@Composable
fun PocketbookTheme(useWallpaperColours: Boolean = true, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        useWallpaperColours && supportsWallpaperColours -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> BrandDark
        else -> BrandLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
