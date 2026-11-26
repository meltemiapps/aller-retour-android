package io.github.allerretour.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.MAX_STOPS
import io.github.allerretour.data.Stop
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.Field
import io.github.allerretour.ui.Footnote
import io.github.allerretour.ui.FormButton
import io.github.allerretour.ui.FormDivider
import io.github.allerretour.ui.FormSection
import io.github.allerretour.ui.FormSheet
import io.github.allerretour.ui.GlassButton
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.Headline
import io.github.allerretour.ui.LabeledRow
import io.github.allerretour.ui.LocalDark
import io.github.allerretour.ui.MenuPicker
import io.github.allerretour.ui.Screen
import io.github.allerretour.ui.Stepper
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.ToggleRow
import io.github.allerretour.ui.ValuePill
import io.github.allerretour.ui.accent
import io.github.allerretour.ui.pickDate
import io.github.allerretour.ui.secondary
import java.time.LocalDate

@Composable
fun PlanScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableIntStateOf(-1) }
    val warnings = TripLogic.routeWarnings(s.stops)

    Screen("Plan") {
        if (s.stops.isEmpty()) {
            GlassCard(title = "Your route") {
                Text("Add up to 10 stops, in any order of countries. Dates for later stops move automatically when you extend a stay.", fontSize = 16.sp)
                GlassButton("Add a stop", prominent = true) { adding = true }
            }
            return@Screen
        }
        val active = vm.activeIndex(s)
        GlassCard(title = "Your route", trailing = "${s.stops.size} of $MAX_STOPS") {
            s.stops.forEachIndexed { i, st ->
                val p = vm.packs[st.country]
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { editing = i }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val on = i == active
                    Box(
                        Modifier.size(40.dp).background(if (on) accent else (if (LocalDark.current) Color.White.copy(alpha = .14f) else Color.White.copy(alpha = .55f)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else LocalContentColor.current) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Headline("${vm.stopName(st)}, ${p.name}")
                        Sub("${TripLogic.pretty(st.from)} – ${TripLogic.pretty(st.to)} · ${TripLogic.nights(st)} nights")
                        warnings[i]?.let { w ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Warning, null, tint = Brand.Warn, modifier = Modifier.padding(top = 2.dp).size(14.dp))
                                Footnote(w, Modifier.padding(start = 5.dp), color = Brand.Warn)
                            }
                        }
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = secondary.copy(alpha = .6f))
                }
            }
            if (s.stops.size < MAX_STOPS) GlassButton("Add a stop", icon = Icons.Outlined.Add) { adding = true }
        }

        vm.activeStop(s)?.let { st ->
            GlassCard(title = "Extend my stay in ${vm.stopName(st)}") {
                Sub("Later stops shift automatically. Schengen days update as you go.")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(prominent = false, desc = "One night fewer") { vm.extendStay(active, -1) }
                    CenterHeadline("Leave ${TripLogic.pretty(st.to)}", Modifier.weight(1f).padding(horizontal = 8.dp))
                    RoundIcon(prominent = true, desc = "One more night") { vm.extendStay(active, 1) }
                }
            }
        }

        GlassCard(title = "Schengen 90/180") {
            val used = TripLogic.schengenMaxDays(s.stops, vm.packs.packs, s.schengenDaysAlreadyUsed)
            Headline("$used of 90 days in any 180-day window")
            Stepper("Days already used on earlier trips: ${s.schengenDaysAlreadyUsed}", s.schengenDaysAlreadyUsed, 0..90) { vm.setSchengenUsed(it) }
            Footnote("Arrival and departure days both count.")
        }
    }

    if (adding) StopEditor(vm, null, -1) { adding = false }
    if (editing in s.stops.indices) StopEditor(vm, s.stops[editing], editing) { editing = -1 }
}

@Composable
private fun CenterHeadline(text: String, modifier: Modifier) =
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = modifier, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

@Composable
private fun RoundIcon(prominent: Boolean, desc: String, onClick: () -> Unit) {
    val dark = LocalDark.current
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .background(if (prominent) accent else if (dark) Color.White.copy(alpha = .12f) else Color.White.copy(alpha = .5f))
            .border(1.dp, Color.White.copy(alpha = .45f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { Icon(if (prominent) Icons.Outlined.Add else Icons.Outlined.Remove, null, tint = if (prominent) Color.White else accent) }
}

/** Add a stop ([stop] == null) or edit one. Same fields and order as StopEditor on iOS. */
@Composable
private fun StopEditor(vm: AppViewModel, stop: Stop?, index: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    val sorted = remember { vm.packs.packs.values.sortedBy { it.name } }
    val start = remember {
        if (stop != null) stop.from to stop.to
        else TripLogic.date(s.stops.lastOrNull()?.to.orEmpty())?.let { it.toString() to it.plusDays(3).toString() }
            ?: (LocalDate.now().toString() to LocalDate.now().plusDays(4).toString())
    }
    var country by remember { mutableStateOf(stop?.country ?: "GR") }
    var city by remember { mutableStateOf(stop?.city ?: "") }
    var from by remember { mutableStateOf(start.first) }
    var to by remember { mutableStateOf(start.second) }
    var shiftLater by remember { mutableStateOf(true) }

    FormSheet(
        title = if (stop == null) "Add a stop" else "Edit stop",
        onDismiss = onDismiss, confirm = "Save",
        onConfirm = {
            if (stop == null) vm.addStop(country, city, from, to) else vm.updateStop(index, country, city, from, to, shiftLater)
            onDismiss()
        },
    ) {
        FormSection {
            LabeledRow("Country") { MenuPicker(sorted.map { it.code }, country, { c -> vm.packs[c].let { "${it.flag} ${it.name}" } }) { country = it } }
            FormDivider()
            Field(city, { city = it }, placeholder = vm.packs[country].defaultCity, modifier = Modifier.padding(vertical = 6.dp))
            FormDivider()
            LabeledRow("Arrive") {
                ValuePill(TripLogic.pretty(from)) {
                    pickDate(context, from) { f -> from = f; if (to < f) to = f }
                }
            }
            FormDivider()
            LabeledRow("Leave") { ValuePill(TripLogic.pretty(to)) { pickDate(context, to, min = from) { to = it } } }
            if (stop != null) {
                FormDivider()
                ToggleRow(shiftLater, { shiftLater = it }) { Text("Move later stops by the same number of days", fontSize = 16.sp) }
            }
        }
        if (stop != null) FormSection {
            FormButton("Remove this stop", color = Brand.Danger) { vm.removeStop(index); onDismiss() }
        }
    }
}
