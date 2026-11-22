@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.pocketbook.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pocketbook.AppViewModel
import io.github.pocketbook.logic.TripLogic
import io.github.pocketbook.ui.Coral
import io.github.pocketbook.ui.Flag
import io.github.pocketbook.ui.InfoCard
import io.github.pocketbook.ui.SectionTitle
import io.github.pocketbook.ui.ShowCard
import io.github.pocketbook.ui.Small
import io.github.pocketbook.ui.Tone
import io.github.pocketbook.ui.dial
import io.github.pocketbook.ui.hasLocationPermission
import io.github.pocketbook.ui.oneOffLocation
import io.github.pocketbook.ui.rememberHeading
import io.github.pocketbook.ui.rememberLiveLocation
import io.github.pocketbook.ui.shareText
import io.github.pocketbook.ui.toast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SafetyScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pack = vm.activePack(s)
    var hereText by remember { mutableStateOf<String?>(null) }
    var hereCoords by remember { mutableStateOf("") }
    var showDriver by remember { mutableStateOf(false) }
    var pinLabel by remember { mutableStateOf("Hotel") }
    var guiding by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) pendingAction?.invoke() else toast(context, "Location permission is needed for this feature only")
        pendingAction = null
    }
    fun withLocation(action: () -> Unit) {
        if (hasLocationPermission(context)) action() else {
            pendingAction = action
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("Emergency · ${pack.name}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pack.emergency.forEachIndexed { i, e ->
                    val main = i == 0
                    Column(
                        Modifier.then(if (main) Modifier.fillMaxWidth() else Modifier.width(160.dp))
                            .background(
                                if (e.primary) Brush.linearGradient(listOf(Color(0xFFD24D2C), Color(0xFFB23A1E))) else Brush.linearGradient(listOf(Color(0xFF2F4B54), Color(0xFF1D343B))),
                                RoundedCornerShape(18.dp),
                            )
                            .clickable { dial(context, "tel:${e.number}") }
                            .padding(14.dp),
                    ) {
                        Text(e.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(e.number, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Small("Tap to open your phone's dialler. Numbers are stored in the app and work offline.", Modifier.padding(top = 8.dp))
        }
        item {
            SectionTitle("Call home")
            InfoCard {
                var num by remember(s.homeNumber) { mutableStateOf(s.homeNumber) }
                OutlinedTextField(
                    num, { num = it; vm.setHomeNumber(it) }, label = { Text("UK number, e.g. 07700 900123") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                )
                val d = TripLogic.ukDial(s.homeNumber, pack.exitCode)
                if (d != null) {
                    Text(
                        "Saved as: ${d.international}\nDialling from ${pack.name}: ${d.fromHere}", fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(10.dp),
                    )
                    val uk = Instant.now().atZone(ZoneId.of("Europe/London")).format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))
                    when (TripLogic.callTime(Instant.now())) {
                        TripLogic.CallTime.GOOD -> Flag("✓ Good time to call ($uk in the UK)", Tone.GOOD)
                        TripLogic.CallTime.BORDERLINE -> Flag("Borderline: it's $uk in the UK", Tone.WARN)
                        TripLogic.CallTime.LATE -> Flag("It's $uk in the UK. They may be asleep", Tone.BAD)
                    }
                    Button(onClick = { dial(context, d.telUri) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Call home") }
                }
            }
        }
        item {
            SectionTitle("\"I am here\"")
            InfoCard {
                Button(
                    onClick = {
                        withLocation {
                            oneOffLocation(context) { loc ->
                                if (loc == null) { toast(context, "Couldn't get a location. Is location turned on?"); return@oneOffLocation }
                                val lat = "%.5f".format(Locale.UK, loc.latitude); val lon = "%.5f".format(Locale.UK, loc.longitude)
                                hereCoords = "$lat, $lon"
                                hereText = "I am here: $lat, $lon\nNear: ${vm.stopName(vm.activeStop(s))}, ${pack.name}\n" +
                                    "Map: https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17\n" +
                                    "Accuracy about ${loc.accuracy.toInt()} m"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral), modifier = Modifier.fillMaxWidth(),
                ) { Text("📍 Get my location now") }
                Small("A one-off GPS fix. Nothing runs in the background, and nothing is sent unless you choose to share it.", Modifier.padding(top = 6.dp))
                hereText?.let { t ->
                    Text(t, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(10.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { shareText(context, t) }, modifier = Modifier.weight(1f)) { Text("Share…") }
                        OutlinedButton(onClick = { showDriver = true }, modifier = Modifier.weight(1f)) { Text("Show driver") }
                    }
                }
            }
        }
        item {
            SectionTitle("Drop a pin")
            InfoCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Hotel", "Parked car", "Meeting point").forEach { l -> FilterChip(selected = pinLabel == l, onClick = { pinLabel = l }, label = { Text(l) }) }
                }
                Button(onClick = {
                    withLocation {
                        oneOffLocation(context) { loc ->
                            if (loc == null) toast(context, "Couldn't get a location. Is location turned on?")
                            else { vm.savePin(pinLabel, loc.latitude, loc.longitude); toast(context, "$pinLabel pin saved on this phone") }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Save pin here") }
                val pin = s.pin
                if (pin == null) Small("No pin saved yet.", Modifier.padding(top = 6.dp)) else {
                    Small("${pin.label} saved at ${"%.5f".format(Locale.UK, pin.lat)}, ${"%.5f".format(Locale.UK, pin.lon)}", Modifier.padding(top = 6.dp))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { if (guiding) guiding = false else withLocation { guiding = true } }, modifier = Modifier.weight(1f)) {
                            Text(if (guiding) "Stop guiding" else "Guide me back")
                        }
                        OutlinedButton(onClick = { guiding = false; vm.clearPin() }, modifier = Modifier.weight(1f)) { Text("Clear pin") }
                    }
                    if (guiding) {
                        val here by rememberLiveLocation(true)
                        val heading by rememberHeading(true)
                        val loc = here
                        if (loc == null) Small("Finding your position…", Modifier.padding(top = 8.dp)) else {
                            val m = TripLogic.distanceMetres(loc.latitude, loc.longitude, pin.lat, pin.lon)
                            val b = TripLogic.bearingDegrees(loc.latitude, loc.longitude, pin.lat, pin.lon)
                            val h = heading
                            Compass(if (h != null) (b - h).toFloat() else b.toFloat())
                            Text(
                                if (m < 15) "You're there 🎉" else "${TripLogic.distanceLabel(m)} ${TripLogic.compassPoint(b)} · about ${maxOf(1, (m / 80).toInt())} min walk",
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(),
                            )
                            if (h == null) Small("No compass on this phone, so the arrow points relative to north.")
                        }
                    }
                }
            }
        }
    }

    if (showDriver) ShowCard("Please take me here", hereCoords, "${vm.stopName(vm.activeStop(s))}, ${pack.name}") { showDriver = false }
}

@Composable
private fun Compass(rotation: Float) {
    val arrow = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(150.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(80.dp).rotate(rotation)) {
                val w = size.width; val h = size.height
                val path = Path().apply {
                    moveTo(w / 2, 0f); lineTo(w * .8f, h); lineTo(w / 2, h * .78f); lineTo(w * .2f, h); close()
                }
                drawPath(path, arrow)
                drawCircle(Color.White, radius = w * .05f, center = Offset(w / 2, h * .55f))
            }
        }
    }
}
