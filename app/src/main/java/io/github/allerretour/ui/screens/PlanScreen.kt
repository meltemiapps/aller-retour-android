@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package io.github.allerretour.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.MAX_STOPS
import io.github.allerretour.data.Stop
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Bullet
import io.github.allerretour.ui.CheckRow
import io.github.allerretour.ui.Flag
import io.github.allerretour.ui.InfoCard
import io.github.allerretour.ui.KeyValue
import io.github.allerretour.ui.SectionTitle
import io.github.allerretour.ui.Small
import io.github.allerretour.ui.Tone
import io.github.allerretour.ui.pickDate
import java.time.LocalDate

@Composable
fun PlanScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableIntStateOf(-1) }
    var adding by remember { mutableStateOf(false) }
    val warnings = TripLogic.routeWarnings(s.stops)
    val packs = vm.packs.packs

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("My route · ${s.stops.size} of $MAX_STOPS stops")
            InfoCard {
                s.stops.forEachIndexed { i, st ->
                    val p = vm.packs[st.country]
                    val on = i == s.activeIndex
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { vm.setActive(i) }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(28.dp).background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("${i + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("${p.flag} ${vm.stopName(st)}", fontWeight = FontWeight.Bold)
                            val n = TripLogic.nights(st)
                            Small("${p.name} · ${TripLogic.pretty(st.from)} – ${TripLogic.pretty(st.to)}" + if (n > 0) " · $n night${if (n > 1) "s" else ""}" else "")
                        }
                        TextButton(onClick = { editing = i }) { Text("✎") }
                        TextButton(onClick = { vm.moveStop(i, -1) }, enabled = i > 0) { Text("↑") }
                        TextButton(onClick = { vm.moveStop(i, 1) }, enabled = i < s.stops.size - 1) { Text("↓") }
                    }
                    warnings[i]?.let { Flag(it, if (it.startsWith("Overlaps")) Tone.BAD else Tone.WARN) }
                }
                val countries = TripLogic.tripCountries(s.stops).map { vm.packs[it] }
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text("${countries.size} countr${if (countries.size == 1) "y" else "ies"}") })
                    AssistChip(onClick = {}, label = { Text(countries.map { it.currency }.distinct().joinToString(" · ")) })
                    AssistChip(onClick = {}, label = { Text("Plugs " + TripLogic.tripPlugs(s.stops, packs).joinToString("/")) })
                    if (countries.any { it.voltage < 200 }) AssistChip(onClick = {}, label = { Text("⚡ 110V stop") })
                }
                if (s.stops.size < MAX_STOPS) {
                    FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("+ Add a stop") }
                } else Small("Maximum of $MAX_STOPS stops reached.")
            }
        }

        if (TripLogic.hasSchengen(s.stops, packs)) item {
            val used = TripLogic.schengenMaxDays(s.stops, packs, s.schengenDaysAlreadyUsed)
            SectionTitle("Schengen 90/180")
            InfoCard {
                Text("$used / 90 days", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Flag(if (used > 90) "Over by ${used - 90} days" else "${90 - used} days left", if (used > 90) Tone.BAD else if (used > 75) Tone.WARN else Tone.GOOD)
                var prior by remember(s.schengenDaysAlreadyUsed) { mutableStateOf(s.schengenDaysAlreadyUsed.toString()) }
                OutlinedTextField(
                    prior, { prior = it.filter(Char::isDigit).take(3); vm.setSchengenUsed(prior.toIntOrNull() ?: 0) },
                    label = { Text("Schengen days already used in the last 180") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Small("Counts every day in a Schengen country, including arrival and departure days, in any rolling 180-day window. It updates when you extend a stop.")
            }
        }

        item {
            SectionTitle("Documents & entry")
            InfoCard {
                OutlinedButton(onClick = { pickDate(context, s.passportExpiry) { vm.setPassportExpiry(it) } }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.passportExpiry.isBlank()) "Set passport expiry date" else "Passport expires ${TripLogic.pretty(s.passportExpiry)}")
                }
                TripLogic.passportChecks(TripLogic.date(s.passportExpiry), s.stops, packs).forEach { (ok, msg) ->
                    Flag((if (ok) "✓ " else "⚠ ") + msg, if (ok) Tone.GOOD else Tone.BAD)
                }
                val countries = TripLogic.tripCountries(s.stops).map { vm.packs[it] }
                countries.filter { it.idp.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { list ->
                    Small("DRIVING PERMITS", Modifier.padding(top = 10.dp))
                    list.forEach { Bullet("${it.flag} ${it.name}: International Driving Permit ${it.idp.lowercase()}") }
                }
                countries.forEach { c ->
                    if (c.entry.isNotEmpty()) {
                        Text("${c.flag} ${c.name}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                        c.entry.forEach { Bullet(it) }
                    }
                }
                Small("Entry rules change. Always confirm on GOV.UK foreign travel advice before you book.", Modifier.padding(top = 8.dp))
            }
        }

        item {
            val items = TripLogic.readinessItems(s.stops, packs)
            val done = items.count { it.first in s.readiness }
            SectionTitle("Ready to go? · $done of ${items.size}")
            InfoCard { items.forEach { (k, label) -> CheckRow(label, k in s.readiness) { vm.toggleReady(k) } } }
        }
    }

    if (adding) StopDialog(vm, null, -1, onDismiss = { adding = false })
    if (editing >= 0 && editing < s.stops.size) StopDialog(vm, s.stops[editing], editing, onDismiss = { editing = -1 })
}

/** Add a new stop ([stop] == null) or edit an existing one, including "extend my stay". */
@Composable
private fun StopDialog(vm: AppViewModel, stop: Stop?, index: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    var country by remember { mutableStateOf(stop?.country ?: vm.activeStop(s)?.country ?: "GR") }
    var city by remember { mutableStateOf(stop?.city ?: "") }
    var from by remember { mutableStateOf(stop?.from ?: s.stops.lastOrNull()?.to.orEmpty()) }
    var to by remember { mutableStateOf(stop?.to ?: "") }
    var shift by remember { mutableStateOf(true) }
    val bad = from.isNotBlank() && to.isNotBlank() && to < from

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (stop == null) "Add a stop" else "Change ${vm.stopName(stop)}") },
        text = {
            Column {
                if (stop == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.packs.packs.values.forEach { p ->
                            FilterChip(selected = country == p.code, onClick = { country = p.code }, label = { Text("${p.flag} ${p.name}") })
                        }
                    }
                }
                OutlinedTextField(city, { city = it }, label = { Text("City or area") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate(context, from) { from = it } }, modifier = Modifier.weight(1f)) { Text(if (from.isBlank()) "Arrive" else TripLogic.pretty(from)) }
                    OutlinedButton(onClick = { pickDate(context, to.ifBlank { from }) { to = it } }, modifier = Modifier.weight(1f)) { Text(if (to.isBlank()) "Leave" else TripLogic.pretty(to)) }
                }
                if (stop != null) {
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { TripLogic.date(to)?.let { to = it.minusDays(1).toString() } }, modifier = Modifier.weight(1f)) { Text("− 1 day") }
                        OutlinedButton(onClick = { to = (TripLogic.date(to) ?: TripLogic.date(from) ?: LocalDate.now()).plusDays(1).toString() }, modifier = Modifier.weight(1f)) { Text("+ 1 day") }
                    }
                    if (index < s.stops.size - 1) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { shift = !shift }) {
                            Checkbox(checked = shift, onCheckedChange = { shift = it })
                            Text("Move later stops by the same number of days", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (bad) Flag("Leave date is before arrival", Tone.BAD)
            }
        },
        confirmButton = {
            Button(enabled = !bad, onClick = {
                if (stop == null) vm.addStop(country, city, from, to) else vm.updateStop(index, city, from, to, shift)
                onDismiss()
            }) { Text(if (stop == null) "Add" else "Save") }
        },
        dismissButton = {
            Row {
                if (stop != null && s.stops.size > 1) TextButton(onClick = { vm.removeStop(index); onDismiss() }) { Text("Remove stop", color = MaterialTheme.colorScheme.secondary) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
