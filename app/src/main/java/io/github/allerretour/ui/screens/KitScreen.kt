@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.allerretour.ui.screens

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
import io.github.allerretour.ui.Bullet
import io.github.allerretour.ui.CheckRow
import io.github.allerretour.ui.InfoCard
import io.github.allerretour.ui.SectionTitle
import io.github.allerretour.ui.Small
import io.github.allerretour.ui.toast
import io.github.allerretour.ui.supportsWallpaperColours

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.material3.MaterialTheme
import io.github.allerretour.logic.ReminderKind
import io.github.allerretour.logic.ReminderPlan
import io.github.allerretour.ui.Flag
import io.github.allerretour.ui.Tone
import io.github.allerretour.ui.pickDate
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun KitScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val packs = vm.packs.packs
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("Packing list")
            InfoCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TripLogic.packingTemplates.keys.forEach { t -> FilterChip(selected = s.packTemplate == t, onClick = { vm.setPackTemplate(t) }, label = { Text(t) }) }
                }
                val list = TripLogic.packingList(s.packTemplate, s.stops, vm.packs.packs)
                val done = s.packed[s.packTemplate].orEmpty()
                list.forEachIndexed { i, item -> CheckRow(item, i in done) { vm.togglePacked(i) } }
                Row(Modifier.fillMaxWidth()) {
                    Small("${done.count { it < list.size }} of ${list.size} packed", Modifier.weight(1f).padding(top = 14.dp))
                    TextButton(onClick = { vm.resetPacked() }) { Text("Reset for next trip") }
                }
            }
        }
        item {
            val items = TripLogic.readinessItems(s.stops, packs)
            val done = items.count { it.first in s.readiness }
            SectionTitle("Ready to go · $done of ${items.size}")
            InfoCard { items.forEach { (k, label) -> CheckRow(label, k in s.readiness) { vm.toggleReady(k) } } }
        }
        item {
            SectionTitle("Passport & entry")
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
                Small("Many countries need 6 months left on arrival. Schengen needs 3 months after you leave and a passport issued in the last 10 years. Entry rules change: confirm on GOV.UK foreign travel advice.", Modifier.padding(top = 8.dp))
            }
        }
        item {
            val planned = ReminderPlan.plan(s, packs).associateBy { it.kind }
            val onCount = ReminderKind.entries.count { ReminderPlan.isOn(s, it) }
            SectionTitle("Reminders · $onCount of ${ReminderKind.entries.size} on")
            InfoCard {
                Small("All optional. Set on this phone only; nothing is sent anywhere.")
                ReminderKind.entries.forEach { k ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(k.title, fontWeight = FontWeight.SemiBold)
                            planned[k]?.let { Small(ReminderPlan.whenLine(it)) }
                        }
                        Switch(checked = ReminderPlan.isOn(s, k), onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                            vm.setReminder(k, on)
                        })
                    }
                }
                val flight = ReminderPlan.flight(s)
                OutlinedButton(onClick = {
                    val start = flight?.toLocalDate()?.toString() ?: TripLogic.tripStart(s.stops)?.toString() ?: LocalDate.now().toString()
                    pickDate(context, start) { d ->
                        val t = flight?.toLocalTime() ?: LocalTime.of(10, 0)
                        TimePickerDialog(context, { _, h, m -> vm.setFlight("${d}T%02d:%02d".format(h, m)) }, t.hour, t.minute, true).show()
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(if (flight == null) "Set outbound flight time" else "Flight ${TripLogic.pretty(flight.toLocalDate().toString())}, %02d:%02d".format(flight.hour, flight.minute))
                }
                OutlinedButton(onClick = { pickDate(context, s.insuranceEnds) { vm.setInsuranceEnds(it) } }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.insuranceEnds.isBlank()) "Set travel insurance end date" else "Insurance ends ${TripLogic.pretty(s.insuranceEnds)}")
                }
                OutlinedButton(onClick = { pickDate(context, s.ghicExpiry) { vm.setGhicExpiry(it) } }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.ghicExpiry.isBlank()) "Set GHIC card expiry date" else "GHIC expires ${TripLogic.pretty(s.ghicExpiry)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val t = ReminderPlan.time(s.medsTime, LocalTime.of(8, 0))
                        TimePickerDialog(context, { _, h, m -> vm.setMedsTime("%02d:%02d".format(h, m)) }, t.hour, t.minute, true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Meds ${s.medsTime}") }
                    OutlinedButton(onClick = {
                        val t = ReminderPlan.time(s.safeTime, LocalTime.of(21, 0))
                        TimePickerDialog(context, { _, h, m -> vm.setSafeTime("%02d:%02d".format(h, m)) }, t.hour, t.minute, true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Safe check-in ${s.safeTime}") }
                }
            }
        }
    }
}
