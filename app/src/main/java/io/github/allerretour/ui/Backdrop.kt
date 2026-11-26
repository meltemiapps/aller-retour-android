package io.github.allerretour.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/** The background choices, in the order shown in Settings. [key] is what is saved. */
enum class BackdropKind(val key: String, val label: String) {
    SKY("Sky", "Sky"), SUNSET("Sunset", "Sunset"), OLIVE("Olive", "Olive"), HARBOUR("Harbour", "Harbour"), PHOTO("Photo", "Your photo");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: SKY
    }
}

/** One scene, ported stop for stop from BackdropView in the iPhone app. */
private class Scene(
    val stops: List<Long>,
    val sun: Long, val sunAlpha: Float, val sunX: Float, val sunY: Float,
    val hill: Long, val hillAlpha: Float,
    val sea: Long, val seaAlpha: Float,
)

private fun scene(kind: BackdropKind, dark: Boolean): Scene = when (kind) {
    BackdropKind.SUNSET -> if (dark) Scene(listOf(0x3A1C3E, 0x5A2A4E, 0x2E2F66, 0x0E1838), 0xFFAA78, .55f, .3f, .55f, 0x140A1E, .6f, 0x14204F, .6f)
    else Scene(listOf(0xFFB49A, 0xF59A8A, 0xCC7FAE, 0x5A78BF, 0x23468A), 0xFFE2A0, .95f, .3f, .55f, 0x50285A, .4f, 0x23468A, .6f)
    BackdropKind.OLIVE -> if (dark) Scene(listOf(0x16241A, 0x22382A, 0x1A2E20, 0x0D1810), 0xE6F0C8, .3f, .75f, .15f, 0x050C06, .6f, 0x1E3723, .6f)
    else Scene(listOf(0xE4F0D8, 0xC4DDA8, 0x93B978, 0x5E8A4C, 0x3E6634), 0xFFFAD7, .95f, .75f, .15f, 0x3E6634, .45f, 0x2E5028, .55f)
    BackdropKind.HARBOUR -> if (dark) Scene(listOf(0x070C1C, 0x0D1730, 0x10203E, 0x050A16), 0xFFC878, .35f, .2f, .2f, 0x000000, .6f, 0x142850, .55f)
    else Scene(listOf(0x8FA6C8, 0x6F87B0, 0x4C6593, 0x2B416E), 0xFFD696, .6f, .2f, .2f, 0x1E2C50, .5f, 0x19284B, .6f)
    else -> if (dark) Scene(listOf(0x0A2240, 0x123E6E, 0x0F3B6B, 0x071A30), 0xD6E9FF, .45f, .78f, .12f, 0x041224, .6f, 0x1A5591, .5f)
    else Scene(listOf(0x9ACFFA, 0xB9DCFA, 0x8EC3F0, 0x5FA3E2, 0x1E74C8), 0xFFF7DC, .95f, .8f, .12f, 0x0D5EAF, .35f, 0x0D5EAF, .55f)
}

private fun rgb(hex: Long, alpha: Float = 1f) = Color(0xFF000000 or hex).copy(alpha = alpha)

private val ridge = listOf(.15f to .03f, .28f to .08f, .42f to 0f, .58f to .07f, .72f to .02f, .85f to .06f, 1f to .01f)

/**
 * What is behind every screen, and what each glass card blurs. Held once at the root so the
 * cards can redraw exactly the part of the background that sits under them.
 */
@Stable
class BackdropState {
    var kind by mutableStateOf(BackdropKind.SKY)
    var dark by mutableStateOf(false)
    var photo by mutableStateOf<ImageBitmap?>(null)
    /** Size of the root, in px. */
    var size by mutableStateOf(Size.Zero)

    fun draw(scope: DrawScope, area: Size = size) {
        val p = photo
        if (kind == BackdropKind.PHOTO && p != null) scope.drawPhoto(p, area) else scope.drawScene(scene(kind, dark), area)
    }
}

val LocalBackdrop = staticCompositionLocalOf { BackdropState() }

private fun DrawScope.drawPhoto(img: ImageBitmap, area: Size) {
    if (area.width <= 0 || area.height <= 0) return
    // Scale to fill, centred, like SwiftUI's scaledToFill().
    val scale = max(area.width / img.width, area.height / img.height)
    val w = img.width * scale; val h = img.height * scale
    drawImage(
        img, dstOffset = IntOffset(((area.width - w) / 2).roundToInt(), ((area.height - h) / 2).roundToInt()),
        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
    )
}

private fun DrawScope.drawScene(sc: Scene, area: Size) {
    val w = area.width; val h = area.height
    if (w <= 0 || h <= 0) return
    drawRect(Brush.verticalGradient(sc.stops.map { rgb(it) }, startY = 0f, endY = h), size = area)
    val sun = rgb(sc.sun, sc.sunAlpha)
    val r = max(w, h) * .35f
    drawRect(Brush.radialGradient(listOf(sun, sun.copy(alpha = 0f)), center = Offset(w * sc.sunX, h * sc.sunY), radius = r), size = area)
    val y0 = h * .62f
    val hill = Path().apply {
        moveTo(0f, y0 + h * .10f)
        ridge.forEach { (x, dy) -> lineTo(w * x, y0 + h * dy) }
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hill, rgb(sc.hill, sc.hillAlpha))
    val s0 = h * .78f
    val sea = Path().apply {
        moveTo(0f, s0)
        quadraticTo(w * .25f, s0 - h * .02f, w * .5f, s0 + h * .01f)
        quadraticTo(w * .75f, s0 + h * .03f, w, s0 - h * .005f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(sea, rgb(sc.sea, sc.seaAlpha))
}

/** Full-bleed background, drawn edge to edge behind the status and navigation bars. */
@Composable
fun Backdrop(state: BackdropState, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) { state.draw(this, size) }
}
