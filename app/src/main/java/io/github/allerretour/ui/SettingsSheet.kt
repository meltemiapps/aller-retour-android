package io.github.allerretour.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.BuildConfig

private const val SOURCE = "github.com/meltemiapps/aller-retour-android"

/** Settings and about, opened from ⓘ. Same sections and order as AboutView on iOS, plus Erase. */
@Composable
fun SettingsSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val photo by vm.photo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = LocalDark.current
    var message by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.savePhoto(uri) { ok -> if (!ok) toast(context, "Couldn't use that photo") }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) message = if (runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) } ?: error("no stream")
            }.isSuccess) "Backup saved." else "Backup not saved."
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            message = if (text != null && vm.importJson(text)) "Backup restored." else "That file isn't an Aller-Retour backup."
        }
    }

    FormSheet(title = "Settings and about", onDismiss = onDismiss, cancel = null, confirm = "Done", onConfirm = onDismiss) {
        FormSection {
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(Brand.hero(dark), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("AR", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Aller-Retour", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Meltemi Apps", color = secondary, fontSize = 16.sp)
                }
            }
            FormDivider()
            Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Headline("Nothing leaves this device")
                Sub("No account, no ads, no analytics, no tracking. The app makes no network connections. Your trips stay on your device.")
            }
        }

        FormSection(
            header = "Appearance",
            footer = "Android doesn't let apps use your wallpaper behind their screens, so pick a background here. Your photo is copied into the app and never leaves this device.",
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Glass", fontSize = 17.sp)
                Spacer(Modifier.width(16.dp))
                Segmented(listOf("Clear", "Tinted"), s.glass, { it }, Modifier.weight(1f)) { vm.setGlass(it) }
            }
            FormDivider()
            val kinds = BackdropKind.entries.filter { it != BackdropKind.PHOTO || photo != null || vm.hasPhoto }
            LabeledRow("Background") {
                MenuPicker(kinds, BackdropKind.of(s.backdrop), { it.label }) { vm.setBackdrop(it.key) }
            }
            FormDivider()
            FormButton("Use one of my photos", Icons.Outlined.Photo) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        FormSection(header = "Backup") {
            FormButton("Export backup", Icons.Outlined.IosShare) { exporter.launch("aller-retour-backup.json") }
            FormDivider()
            FormButton("Restore from backup", Icons.Outlined.FileDownload) { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
            message?.let { FormDivider(); Footnote(it, Modifier.padding(vertical = 8.dp)) }
            FormDivider()
            FormButton("Erase all my data", Icons.Outlined.DeleteForever, color = Brand.Danger) { confirmErase = true }
        }

        FormSection(header = "About", footer = "Country information is a guide only. Check official government travel advice before you travel.") {
            FormValue("Licence", "GNU GPL v3 · free forever")
            FormDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { openLink(context, "https://$SOURCE") }, verticalAlignment = Alignment.CenterVertically) {
                Text("Source code", fontSize = 17.sp, modifier = Modifier.padding(end = 12.dp))
                Spacer(Modifier.weight(1f))
                Text(SOURCE, fontSize = 15.sp, color = accent)
            }
            FormDivider()
            FormValue("Privacy", "No data collected")
        }
    }

    if (confirmErase) AlertDialog(
        onDismissRequest = { confirmErase = false },
        title = { Text("Erase everything?") },
        text = { Text("Your route, spending, checklists, settings and background photo will be deleted from this phone. This can't be undone unless you have exported a backup.") },
        confirmButton = { TextButton(onClick = { vm.eraseEverything(); confirmErase = false; message = null }) { Text("Erase", color = Brand.Danger) } },
        dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
    )
}
