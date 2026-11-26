package io.github.allerretour.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.logic.ReminderKind
import io.github.allerretour.logic.ReminderPlan
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.CheckRow
import io.github.allerretour.ui.Field
import io.github.allerretour.ui.Footnote
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.Inset
import io.github.allerretour.ui.LabeledRow
import io.github.allerretour.ui.Progress
import io.github.allerretour.ui.Screen
import io.github.allerretour.ui.Segmented
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.ToggleRow
import io.github.allerretour.ui.ValuePill
import io.github.allerretour.ui.pickDate
import io.github.allerretour.ui.pickTime
import io.github.allerretour.ui.secondary
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun KitScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val packs = vm.packs.packs
    val list = TripLogic.packingList(s.packTemplate, s.stops, packs)
    val packed = s.packed[s.packTemplate].orEmpty()
    val packedCount = packed.count { it < list.size }
    val ready = TripLogic.readinessItems(s.stops, packs)

    Screen("Kit") {
        GlassCard(title = "Packing list", trailing = "$packedCount of ${list.size} packed") {
            Segmented(TripLogic.packingTemplates.keys.toList(), s.packTemplate, { it }) { vm.setPackTemplate(it) }
            Progress(packedCount / list.size.coerceAtLeast(1).toFloat())
            list.forEachIndexed { i, item -> CheckRow(item, i in packed) { vm.togglePacked(i) } }
        }
        GlassCard(title = "Ready to go", trailing = "${ready.count { it.first in s.readiness }} of ${ready.size} done") {
            ready.forEach { (key, label) -> CheckRow(label, key in s.readiness, round = false) { vm.toggleReady(key) } }
        }
        Passport(vm)
        Reminders(vm)
        GlassCard(title = "Medical card") {
            Field(s.medicalNote, { vm.setMedicalNote(it) }, "Allergies, conditions, medicines, blood type, emergency contact", singleLine = false, minLines = 3)
            Footnote("Kept on this device only. Add it to your phone's emergency information if you want it on the lock screen.")
        }
    }
}

@Composable
private fun Passport(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exp = TripLogic.date(s.passportExpiry)
    val checks = TripLogic.passportChecks(exp, s.stops, vm.packs.packs)
    val months = TripLogic.monthsAfterTrip(exp, s.stops)
    GlassCard(title = "Passport") {
        LabeledRow("Expires") {
            ValuePill(if (exp == null) "Add date" else TripLogic.pretty(s.passportExpiry)) {
                pickDate(context, s.passportExpiry.ifBlank { LocalDate.now().plusYears(5).toString() }) { vm.setPassportExpiry(it) }
            }
        }
        if (exp == null) {
            Sub("Add your expiry date to check it against your route and get renewal reminders.")
        } else {
            months?.let { m ->
                Inset(tint = if (m >= 6) Brand.Good else Brand.Warn) {
                    Text(
                        when {
                            m >= 6 -> "$m months left after you get home. Fine for this trip."
                            m >= 3 -> "Only $m months left after you get home. Under the 6 months many countries require. Renew soon."
                            else -> "Less than 3 months left after this trip. Renew before you travel."
                        }, fontSize = 15.sp,
                    )
                }
            }
            checks.forEach { (ok, msg) ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = if (ok) Brand.Good else Brand.Warn, modifier = Modifier.padding(top = 1.dp).size(18.dp))
                    Text(msg, fontSize = 15.sp, color = if (ok) Brand.Good else Brand.Warn, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Footnote("Many countries need 6 months left on arrival. Schengen needs 3 months after you leave and a passport issued in the last 10 years.")
    }
}

@Composable
private fun Reminders(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val planned = ReminderPlan.plan(s, vm.packs.packs).associateBy { it.kind }
    val onCount = ReminderKind.entries.count { ReminderPlan.isOn(s, it) }

    GlassCard(title = "Reminders", trailing = "$onCount of ${ReminderKind.entries.size} on") {
        Footnote("All optional. Scheduled on this device only; nothing is sent to a server.")
        ReminderKind.entries.forEach { k ->
            ToggleRow(ReminderPlan.isOn(s, k), { on ->
                if (on && Build.VERSION.SDK_INT >= 33) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                vm.setReminder(k, on)
            }) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(k.title, fontSize = 16.sp)
                    planned[k]?.let { Footnote(ReminderPlan.whenLine(it)) }
                }
            }
        }
        HorizontalDivider(thickness = .5.dp, color = secondary.copy(alpha = .35f))

        val flight = ReminderPlan.flight(s)
        LabeledRow("Outbound flight") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ValuePill(flight?.let { TripLogic.pretty(it.toLocalDate().toString()) } ?: "Add date") {
                    val day = flight?.toLocalDate()?.toString() ?: TripLogic.tripStart(s.stops)?.toString() ?: LocalDate.now().toString()
                    pickDate(context, day) { d -> vm.setFlight("${d}T%02d:%02d".format(flight?.hour ?: 10, flight?.minute ?: 0)) }
                }
                ValuePill(flight?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "Time") {
                    val t = flight?.toLocalTime() ?: LocalTime.of(10, 0)
                    val day = flight?.toLocalDate() ?: TripLogic.tripStart(s.stops) ?: LocalDate.now()
                    pickTime(context, t) { nt -> vm.setFlight("${day}T%02d:%02d".format(nt.hour, nt.minute)) }
                }
            }
        }
        DateRow("Travel insurance ends", s.insuranceEnds) { vm.setInsuranceEnds(it) }
        DateRow("GHIC card expires", s.ghicExpiry) { vm.setGhicExpiry(it) }
        TimeRow("Medication", s.medsTime, LocalTime.of(8, 0), Modifier) { vm.setMedsTime(it) }
        TimeRow("Safe check-in", s.safeTime, LocalTime.of(21, 0), Modifier) { vm.setSafeTime(it) }
    }
}

@Composable
private fun DateRow(label: String, value: String, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    LabeledRow(label) { ValuePill(if (value.isBlank()) "Add date" else TripLogic.pretty(value)) { pickDate(context, value, onPicked = onPicked) } }
}

@Composable
private fun TimeRow(label: String, value: String, fallback: LocalTime, modifier: Modifier, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    val t = ReminderPlan.time(value, fallback)
    LabeledRow(label, modifier) { ValuePill("%02d:%02d".format(t.hour, t.minute)) { pickTime(context, t) { onPicked("%02d:%02d".format(it.hour, it.minute)) } } }
}
