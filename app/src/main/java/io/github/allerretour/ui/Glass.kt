package io.github.allerretour.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "Clear" lets the background show through; "Tinted" is easier to read. Set in Settings. */
val LocalTinted = staticCompositionLocalOf { false }

/** iOS-style secondary text colour. */
val secondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val accent: Color @Composable get() = MaterialTheme.colorScheme.primary

/**
 * Translucent glass: white (light) or navy (dark) over the background, with a thin white edge.
 * On Android 12 and later the background under the glass is blurred as well.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tinted: Boolean = LocalTinted.current,
    edge: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val dark = LocalDark.current
    val backdrop = LocalBackdrop.current
    val pos = remember { mutableStateOf(Offset.Zero) }
    val fill = (if (dark) Brand.Navy else Color.White).copy(alpha = if (tinted) .70f else .35f)
    Box(modifier.onGloballyPositioned { pos.value = it.positionInRoot() }.clip(shape)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer { renderEffect = BlurEffect(28.dp.toPx(), 28.dp.toPx(), TileMode.Clamp) }
                    .drawBehind { val p = pos.value; translate(-p.x, -p.y) { backdrop.draw(this) } },
            )
        }
        Box(Modifier.matchParentSize().background(fill, shape).then(if (edge) Modifier.border(1.dp, Color.White.copy(alpha = .40f), shape) else Modifier))
        content()
    }
}

/** Port of GlassCard: rounded 26dp, padding 18dp, optional small-caps title and trailing value. */
@Composable
fun GlassCard(
    title: String? = null,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null || trailing != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (title != null) Text(
                        title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                        color = secondary, modifier = Modifier.weight(1f),
                    ) else Spacer(Modifier.weight(1f))
                    if (trailing != null) Text(trailing, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }
            content()
        }
    }
}

/** Standard screen: large title top left, scrolling cards. The background and the ⓘ button are drawn by the root. */
@Composable
fun Screen(title: String, content: @Composable ColumnScope.() -> Unit) {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = nav + 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            title, fontSize = 34.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, end = 60.dp).heightIn(min = 44.dp).semantics { heading() },
        )
        content()
    }
}

/** The round ⓘ glass button that opens Settings and about. */
@Composable
fun InfoButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassSurface(modifier.size(44.dp), CircleShape, tinted = true) {
        Box(
            Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = "Settings and about" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Info, null, tint = accent) }
    }
}

/** Buttons in the two iOS glass styles: prominent (filled with the tint) or plain glass. */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    tint: Color = accent,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val dark = LocalDark.current
    val bg = if (prominent) tint else (if (dark) Color.White.copy(alpha = .12f) else Color.White.copy(alpha = .45f))
    val fg = if (prominent) Color.White else tint
    Row(
        modifier.alpha(if (enabled) 1f else .45f).heightIn(min = 40.dp).clip(CircleShape).background(bg)
            .border(1.dp, Color.White.copy(alpha = if (prominent) .25f else .5f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

private fun Modifier.alpha(a: Float) = this.graphicsLayer { alpha = a }

/** iOS segmented control. Scrolls sideways when there are more options than fit. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    val dark = LocalDark.current
    val scroll = options.size > 4
    Row(
        modifier.fillMaxWidth().clip(CircleShape)
            .background(if (dark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .07f))
            .then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier).padding(3.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                (if (scroll) Modifier else Modifier.weight(1f)).clip(CircleShape)
                    .background(if (on) (if (dark) Color(0xFF3A5677) else Color.White) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(o) }.padding(horizontal = if (scroll) 14.dp else 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label(o), fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1) }
        }
    }
}

/** A menu-style picker: shows the choice, opens a list. */
@Composable
fun <T> MenuPicker(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.clip(RoundedCornerShape(10.dp)).clickable { open = true }.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label(selected), color = accent, fontSize = 16.sp)
            Icon(Icons.Outlined.UnfoldMore, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(label(o)) }, onClick = { onSelect(o); open = false }) }
        }
    }
}

/** Rounded text field, like SwiftUI's .roundedBorder style. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val dark = LocalDark.current
    val color = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value, onValueChange, singleLine = singleLine, minLines = minLines,
        textStyle = TextStyle(color = color, fontSize = 16.sp),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                    .background(if (dark) Color.White.copy(alpha = .10f) else Color.White.copy(alpha = .85f))
                    .border(BorderStroke(.5.dp, if (dark) Color.White.copy(alpha = .2f) else Color.Black.copy(alpha = .15f)), RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = secondary.copy(alpha = .8f), fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
fun Progress(fraction: Float, modifier: Modifier = Modifier, color: Color = accent) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) }, color = color, trackColor = LocalContentColor.current.copy(alpha = .14f),
        drawStopIndicator = {}, gapSize = 0.dp,
        modifier = modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
    )
}

@Composable
fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { t ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 8.dp, end = 10.dp).size(6.dp).background(accent, CircleShape))
                Text(t, fontSize = 16.sp)
            }
        }
    }
}

/** Round check (packing list) or square check (ready to go). */
@Composable
fun CheckRow(title: String, on: Boolean, round: Boolean = true, note: String? = null, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Checkbox, onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (on) (if (round) Icons.Filled.CheckCircle else Icons.Filled.CheckBox) else (if (round) Icons.Outlined.RadioButtonUnchecked else Icons.Outlined.CheckBoxOutlineBlank),
            null, tint = if (on) accent else secondary, modifier = Modifier.size(24.dp),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                title, fontSize = 16.sp, color = if (on && round) secondary else LocalContentColor.current,
                textDecoration = if (on && round) TextDecoration.LineThrough else null,
            )
            if (note != null) Footnote(note)
        }
    }
}

@Composable
fun Footnote(text: String, modifier: Modifier = Modifier, color: Color = secondary) =
    Text(text, fontSize = 13.sp, color = color, modifier = modifier)

@Composable
fun Sub(text: String, modifier: Modifier = Modifier, color: Color = secondary) =
    Text(text, fontSize = 15.sp, color = color, modifier = modifier)

@Composable
fun Headline(text: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) =
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = modifier)

/** A soft inset panel inside a card, like .thinMaterial on iOS, optionally tinted. */
@Composable
fun Inset(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalDark.current
    val bg = tint?.copy(alpha = .18f) ?: if (dark) Color.White.copy(alpha = .08f) else Color.White.copy(alpha = .5f)
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(12.dp), content = content)
}

/** Label on the left, control on the right, like a Form row. */
@Composable
fun LabeledRow(label: String, modifier: Modifier = Modifier, control: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        control()
    }
}

/** A date or time value shown as a tappable pill, like the compact iOS DatePicker. */
@Composable
fun ValuePill(text: String, onClick: () -> Unit) {
    val dark = LocalDark.current
    Text(
        text, fontSize = 16.sp, color = accent,
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (dark) Color.White.copy(alpha = .12f) else Color.Black.copy(alpha = .06f))
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
fun ToggleRow(checked: Boolean, onChange: (Boolean) -> Unit, label: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(end = 12.dp)) { label() }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Brand.Good, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = LocalContentColor.current.copy(alpha = .16f), uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** iOS Stepper: label, then − and + in one capsule. */
@Composable
fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val dark = LocalDark.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(9.dp)).background(if (dark) Color.White.copy(alpha = .12f) else Color.Black.copy(alpha = .06f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperHalf("−", value > range.first, "Fewer") { onChange(value - 1) }
            Box(Modifier.width(1.dp).height(18.dp).background(secondary.copy(alpha = .4f)))
            StepperHalf("+", value < range.last, "More") { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperHalf(text: String, enabled: Boolean, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 46.dp, height = 34.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 20.sp, color = if (enabled) LocalContentColor.current else secondary.copy(alpha = .5f)) }
}

@Composable
fun WithContentColor(color: Color, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalContentColor provides color, content = content)

val CardSpacing: Dp = 14.dp
