package io.github.allerretour.ui.screens

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.CountryPack
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.Bullets
import io.github.allerretour.ui.Field
import io.github.allerretour.ui.Footnote
import io.github.allerretour.ui.GlassButton
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.Headline
import io.github.allerretour.ui.Inset
import io.github.allerretour.ui.Screen
import io.github.allerretour.ui.Segmented
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.accent
import io.github.allerretour.ui.dial
import io.github.allerretour.ui.hasLocationPermission
import io.github.allerretour.ui.oneOffLocation
import io.github.allerretour.ui.secondary
import io.github.allerretour.ui.shareText
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class GuideSection(val title: String) {
    SAFETY("Safety & emergencies"), ALL("All"), ESSENTIALS("Essentials"), ENTRY("Entry"), MONEY("Money"),
    CUSTOMS("Customs"), DRIVING("Driving"), HEALTH("Health"), PHRASES("Phrases"), MENU("Menu"),
}

@Composable
fun GuideScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf<String?>(null) }
    var section by rememberSaveable { mutableStateOf(GuideSection.ALL) }
    var query by rememberSaveable { mutableStateOf("") }

    val trip = TripLogic.tripCountries(s.stops).mapNotNull { vm.packs.packs[it] }
    val countries = trip.ifEmpty { vm.packs.packs.values.sortedBy { it.name } }
    val p = vm.packs[code ?: vm.activeStop(s)?.country ?: countries.firstOrNull()?.code ?: "GR"]
    fun show(x: GuideSection) = section == GuideSection.ALL || section == x

    Screen("Guide") {
        Segmented(countries.map { it.code }, p.code, { c -> vm.packs[c].let { "${it.flag} ${it.name}" } }) { code = it; query = "" }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuideSection.entries.forEach { x -> GlassButton(x.title, prominent = section == x) { section = x } }
        }

        if (show(GuideSection.SAFETY)) SafetyCards(vm, p)

        if (show(GuideSection.ESSENTIALS)) {
            GlassCard(title = "${p.name} at a glance") {
                FactRow("Language", p.language, "Currency", "${p.currency} (${p.currencySymbol})")
                FactRow("Plugs", "Type " + p.plugs.joinToString(" / "), "Power", "${p.voltage}V · ${p.frequency}Hz")
                FactRow("Drive on the", p.driving.side, "Dial the UK", "${p.exitCode} 44 …")
                Sub(p.water)
            }
            // The Safety section above already lists these, so only show them again under Essentials.
            if (section == GuideSection.ESSENTIALS) EmergencyCard(p, "Emergency numbers", big = false)
        }
        if (show(GuideSection.ENTRY)) GlassCard(title = "Entry and documents") {
            Inset {
                Text(
                    when {
                        p.schengen -> "Schengen country: your passport must be under 10 years old on entry and valid for 3 months after you leave the Schengen area."
                        p.passportMonthsOnArrival > 0 -> "Your passport needs at least ${p.passportMonthsOnArrival} months left on arrival."
                        else -> "Check the passport validity rules before you travel."
                    }, fontSize = 15.sp,
                )
            }
            Bullets(p.entry)
            if (p.idp.isNotBlank()) Text(p.idp, fontSize = 15.sp)
        }
        if (show(GuideSection.MONEY)) GlassCard(title = "Money and tipping") {
            Headline("${p.currency} · indicative £1 = ${p.currencySymbol}${String.format(Locale.UK, "%.2f", p.indicativeRatePerGbp)}")
            Bullets(p.tipping)
        }
        if (show(GuideSection.CUSTOMS)) {
            if (p.etiquette.isNotEmpty()) GlassCard(title = "Etiquette") { Bullets(p.etiquette) }
            if (p.avoid.isNotEmpty()) GlassCard(title = "Things to avoid") { Bullets(p.avoid) }
            if (p.scams.isNotEmpty()) GlassCard(title = "Common scams") { Bullets(p.scams) }
        }
        if (show(GuideSection.DRIVING)) GlassCard(title = "Driving") {
            BoldLead("Drive on the", p.driving.side.lowercase())
            BoldLead("Speed limits", p.driving.speedLimits)
            BoldLead("Must carry", p.driving.mustCarry)
            Bullets(p.driving.notes)
        }
        if (show(GuideSection.HEALTH)) GlassCard(title = "Medicines and health") {
            Text(p.medicines, fontSize = 16.sp)
            if (p.eu) Sub("Use your GHIC for state healthcare. Pharmacies show a green cross.")
        }
        if (show(GuideSection.PHRASES)) GlassCard(title = "${p.language} phrases · tap to show in large text") {
            p.phrases.forEach { ph ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { vm.phrase = ph },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Headline(ph.english)
                        Sub(listOf(ph.local, ph.pronunciation).filter { it.isNotBlank() }.joinToString(" · "))
                    }
                    Icon(Icons.Outlined.OpenInFull, "Show in large text", tint = accent, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (show(GuideSection.MENU)) GlassCard(title = "Menu decoder") {
            Field(query, { query = it }, "Type a word from the menu")
            val q = query.trim().lowercase()
            val items = p.menu.filter { q.isEmpty() || "${it.term} ${it.romanised} ${it.meaning}".lowercase().contains(q) }
            if (items.isEmpty()) Text("No match. Try part of the word.", color = secondary, fontSize = 16.sp)
            items.forEach { m ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Headline(if (m.romanised.isBlank()) m.term else "${m.term} · ${m.romanised}")
                    Sub(m.meaning)
                }
            }
        }
    }
}

@Composable
private fun FactRow(k1: String, v1: String, k2: String, v2: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Fact(k1, v1, Modifier.weight(1f)); Fact(k2, v2, Modifier.weight(1f))
    }
}

@Composable
private fun Fact(k: String, v: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(k, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = secondary)
        Text(v, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BoldLead(lead: String, rest: String) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
        append(" "); append(rest)
    }, fontSize = 16.sp)
}

@Composable
private fun EmergencyCard(p: CountryPack, title: String, big: Boolean) {
    val context = LocalContext.current
    GlassCard(title = title) {
        p.emergency.forEach { e ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (big) 40.dp else 36.dp).clickable(role = Role.Button) { dial(context, "tel:${e.number}") },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    e.number, fontSize = if (big) 20.sp else 17.sp, fontWeight = if (big) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (e.primary) Brand.Danger else accent,
                )
            }
        }
    }
}

private val pinStamp = DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm", Locale.UK)

/** Emergency numbers, call home, "I am here" and drop a pin, for one country. Port of SafetyCards. */
@Composable
private fun SafetyCards(vm: AppViewModel, p: CountryPack) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var last by remember { mutableStateOf<Location?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pinning by remember { mutableStateOf(false) }

    fun fetch() = oneOffLocation(context) { loc ->
        busy = false
        if (loc == null) { error = "Couldn't get a fix. Try again near a window or outdoors."; pinning = false; return@oneOffLocation }
        last = loc
        if (pinning) {
            vm.savePin("Pinned ${LocalDateTime.now().format(pinStamp)}", loc.latitude, loc.longitude)
            pinning = false
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) fetch() else { busy = false; pinning = false; error = "Location is off for Aller-Retour. Turn it on in Settings to use this." }
    }
    fun request() {
        error = null; busy = true
        if (hasLocationPermission(context)) fetch()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    EmergencyCard(p, "Emergency numbers · ${p.name}", big = true)

    GlassCard(title = "Call home") {
        Field(s.homeNumber, { vm.setHomeNumber(it) }, "UK number to call", keyboard = KeyboardType.Phone)
        val d = TripLogic.ukDial(s.homeNumber, p.exitCode)
        if (d != null) {
            Text("From ${p.name}, dial ${d.fromHere} or ${d.international}", fontSize = 15.sp)
            val ct = TripLogic.callTime(Instant.now())
            val good = ct == TripLogic.CallTime.GOOD
            val color = if (good) Brand.Good else Brand.Warn
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (good) Icons.Outlined.CheckCircle else Icons.Outlined.DarkMode, null, tint = color, modifier = Modifier.size(16.dp))
                Footnote(
                    " " + when (ct) {
                        TripLogic.CallTime.GOOD -> "Good time to call the UK"
                        TripLogic.CallTime.BORDERLINE -> "It's early or late in the UK"
                        TripLogic.CallTime.LATE -> "It's night in the UK"
                    }, color = color,
                )
            }
            GlassButton("Call home", prominent = true) { dial(context, d.telUri) }
        } else Footnote("Save a number once and the app formats it for dialling from wherever you are.")
    }

    GlassCard(title = "I am here") {
        val l = last
        if (l != null) {
            val text = String.format(Locale.UK, "My location: %.5f, %.5f (accuracy about %.0f m)", l.latitude, l.longitude, l.accuracy)
            Text(text, fontSize = 16.sp)
            GlassButton("Share as text", icon = Icons.Outlined.IosShare) { shareText(context, text) }
        } else Text("Shows your position as text, ready to read out or send. Only when you tap.", color = secondary, fontSize = 16.sp)
        error?.let { Footnote(it, color = Brand.Warn) }
        GlassButton(if (busy) "Finding you…" else "Show where I am", prominent = true, icon = Icons.Outlined.MyLocation, enabled = !busy) { pinning = false; request() }
    }

    GlassCard(title = "Drop a pin") {
        val pin = s.pin
        if (pin != null) {
            Headline(pin.label)
            val l = last
            if (l != null) {
                val dist = TripLogic.distanceMetres(l.latitude, l.longitude, pin.lat, pin.lon)
                val b = TripLogic.bearingDegrees(l.latitude, l.longitude, pin.lat, pin.lon)
                Text("${TripLogic.distanceLabel(dist)} to the ${TripLogic.compassPoint(b)} (${b.toInt()}°)", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else Text("Tap Update to see how far and which way.", color = secondary, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Update", enabled = !busy) { request() }
                GlassButton("Clear pin", tint = Brand.Danger) { vm.clearPin() }
            }
        } else {
            Text("Save where you parked or your hotel, then find your way back without a map or data.", color = secondary, fontSize = 16.sp)
            GlassButton("Pin this spot", prominent = true, icon = Icons.Outlined.LocationOn, enabled = !busy) { pinning = true; request() }
        }
    }
}
