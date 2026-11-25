@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.allerretour.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.BuildConfig
import io.github.allerretour.logic.TripLogic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

/** Settings and about: appearance, backup, privacy and version. Opened from the ⓘ button in the header. */
@Composable
fun AboutDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmErase by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
        }.onSuccess { toast(context, "Backup saved") }.onFailure { toast(context, "Couldn't save the backup") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            toast(context, if (text != null && vm.importJson(text)) "Backup restored" else "That file isn't a Aller-Retour backup")
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            item { Text("Settings and about", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
        item {
                SectionTitle("Appearance")
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Use wallpaper colours", fontWeight = FontWeight.Bold)
                            Small(
                                if (supportsWallpaperColours) "Matches your phone's colours. Turn off to use Aller-Retour blue."
                                else "Needs Android 12 or later. This phone uses Aller-Retour blue.",
                            )
                        }
                        Switch(
                            checked = s.useWallpaperColours && supportsWallpaperColours,
                            onCheckedChange = { vm.setWallpaperColours(it) },
                            enabled = supportsWallpaperColours,
                        )
                    }
                }
            }
            item {
                SectionTitle("Your data")
                InfoCard {
                    Text("Everything is stored on this phone only", fontWeight = FontWeight.Bold)
                    Small("It is not included in Google backups. If you lose or reset your phone, only an exported backup can bring it back.")
                    Button(onClick = { exporter.launch("aller-retour-backup.json") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Export backup") }
                    OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore from backup") }
                    Small("The backup is a plain JSON file you can read yourself. Keep it somewhere safe.", Modifier.padding(top = 6.dp))
                    TextButton(onClick = { confirmErase = true }) { Text("Erase all my data") }
                }
            }
            item {
                SectionTitle("Privacy")
                InfoCard {
                    Bullet("No internet permission: the app cannot connect to anything")
                    Bullet("No accounts, ads, analytics or crash reporting")
                    Bullet("Location is used only when you tap a location button, and never in the background")
                    Bullet("Calls open your own dialler, and sharing uses Android's share sheet. You see everything before it is sent")
                }
            }
            item {
                SectionTitle("About")
                InfoCard {
                    Text("Aller-Retour ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                    Small("Free and open source under the GNU GPL v3. Country information is a summary for guidance only. Always check official sources such as GOV.UK foreign travel advice.")
                }
            }
        }
    }

    if (confirmErase) AlertDialog(
        onDismissRequest = { confirmErase = false },
        title = { Text("Erase everything?") },
        text = { Text("Your route, spending, checklists and settings will be deleted from this phone. This can't be undone unless you have exported a backup.") },
        confirmButton = { TextButton(onClick = { vm.eraseEverything(); confirmErase = false }) { Text("Erase") } },
        dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
    )
}
