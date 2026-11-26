@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.allerretour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An iOS-style sheet holding a grouped form: Cancel on the left, title in the middle,
 * the confirm action on the right.
 */
@Composable
fun FormSheet(
    title: String,
    onDismiss: () -> Unit,
    cancel: String? = "Cancel",
    confirm: String,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(Modifier.imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { if (cancel != null) SheetAction(cancel, bold = false, onClick = onDismiss) }
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { SheetAction(confirm, bold = true, enabled = confirmEnabled, onClick = onConfirm) }
                }
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SheetAction(text: String, bold: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text, color = if (enabled) accent else secondary, fontSize = 17.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

/** One rounded group of rows, with an optional small-caps header and a footnote below. */
@Composable
fun FormSection(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        if (header != null) Text(
            header.uppercase(), fontSize = 13.sp, color = secondary, letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            content = content,
        )
        if (footer != null) Footnote(footer, Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
    }
}

@Composable
fun FormDivider() = HorizontalDivider(Modifier.padding(vertical = 2.dp), thickness = .5.dp, color = secondary.copy(alpha = .3f))

/** A row that acts like a button: optional icon, accent label (or danger). */
@Composable
fun FormButton(text: String, icon: ImageVector? = null, color: Color = accent, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)) }
        Text(text, color = color, fontSize = 17.sp)
    }
}

/** Label on the left, value on the right in secondary text. */
@Composable
fun FormValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.padding(end = 12.dp))
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 17.sp, color = secondary, textAlign = TextAlign.End)
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
