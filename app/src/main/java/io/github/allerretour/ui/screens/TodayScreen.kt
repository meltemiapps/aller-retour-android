@file:OptIn(ExperimentalLayoutApi::class)

package io.github.allerretour.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.CountryPack
import io.github.allerretour.data.Home
import io.github.allerretour.data.Stop
import io.github.allerretour.logic.ReminderPlan
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.Footnote
import io.github.allerretour.ui.GlassButton
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.Headline
import io.github.allerretour.ui.LocalDark
import io.github.allerretour.ui.Progress
import io.github.allerretour.ui.Screen
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.accent
import io.github.allerretour.ui.dial
import io.github.allerretour.ui.secondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun TodayScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    Screen("Today") {
        val stop = vm.activeStop(s)
        if (stop == null) {
            EmptyTrip(vm)
            return@Screen
        }
        val p = vm.activePack(s)
        Hero(vm, stop, p)

        // Schengen 90/180
        val used = TripLogic.schengenMaxDays(s.stops, vm.packs.packs, s.schengenDaysAlreadyUsed)
        GlassCard(title = "Schengen 90/180") {
            if (TripLogic.hasSchengen(s.stops, vm.packs.packs)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) { append("$used") }
                    withStyle(SpanStyle(color = secondary, fontSize = 17.sp)) { append(" days used") }
                })
                Progress(used.coerceAtMost(90) / 90f, color = if (used > 80) Brand.Danger else accent)
                Sub(if (used > 90) "Over the limit by ${used - 90} days. Change your dates." else "${90 - used} days left in this window")
            } else Text("No Schengen countries on this trip.", color = secondary, fontSize = 16.sp)
        }

        // Quick convert
        val r = vm.rate(s, p)
        GlassCard(title = "Quick convert") {
            Text("£20 in ${p.currency}", color = secondary, fontSize = 16.sp)
            Text(TripLogic.money(20 * r, p.currencySymbol, p.currency), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = accent)
            Footnote("Your rate · works offline")
        }

        // Emergency
        val context = LocalContext.current
        val main = p.emergency.firstOrNull { it.primary } ?: p.emergency.firstOrNull()
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(Brand.Danger, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MedicalServices, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Headline("Emergency: ${main?.number ?: "112"}")
                    Sub(p.emergency.filter { !it.primary }.take(3).joinToString(" · ") { "${it.label} ${it.number}" })
                }
                if (main != null) GlassButton("Call", prominent = true, tint = Brand.Danger) { dial(context, "tel:${main.number}") }
            }
        }

        // Coming up
        val upcoming = ReminderPlan.upcoming(s, vm.packs.packs)
        GlassCard(title = "Coming up") {
            if (upcoming.isEmpty()) Text("No reminders scheduled. Turn some on in Kit.", color = secondary, fontSize = 16.sp)
            upcoming.forEach { u ->
                Column {
                    Text(u.kind.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Footnote(ReminderPlan.whenLine(u))
                }
            }
        }

        // Phrase of the day
        GlassCard(title = "Phrase of the day") {
            if (p.phrases.isNotEmpty()) {
                val ph = p.phrases[LocalDate.now().dayOfYear % p.phrases.size]
                Text(ph.local, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(listOf(ph.pronunciation, ph.english).filter { it.isNotBlank() }.joinToString(" · "), color = secondary, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Hero(vm: AppViewModel, stop: Stop, p: CountryPack) {
    val s by vm.state.collectAsStateWithLifecycle()
    val dark = LocalDark.current
    val idx = vm.activeIndex(s)
    val start = TripLogic.tripStart(s.stops)
    val end = TripLogic.tripEnd(s.stops)
    val today = LocalDate.now()
    val dayN = start?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 } ?: 1
    val total = if (start != null && end != null) ChronoUnit.DAYS.between(start, end).toInt() + 1 else 0
    val zone = ZoneId.of(p.timeZone)
    val hhmm = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    val sunset = TripLogic.sunTimes(p.lat, p.lon, today)?.second?.atZone(zone)?.format(hhmm)
    val next = s.stops.getOrNull(idx + 1)
    val line = listOfNotNull(
        sunset?.let { "Sunset $it" },
        TripLogic.hoursLabel(TripLogic.hoursVsUk(p.timeZone, Instant.now())),
        next?.let { "Next: ${vm.stopName(it).ifBlank { vm.packs[it.country].name }} on ${TripLogic.pretty(it.from)}" },
    ).joinToString(" · ")

    Column(
        Modifier.fillMaxWidth().background(Brand.hero(dark), RoundedCornerShape(30.dp)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (dayN in 1..total) "DAY $dayN OF $total · STOP ${idx + 1} OF ${s.stops.size}" else "STOP ${idx + 1} OF ${s.stops.size} · ${TripLogic.pretty(stop.from)}",
            color = Color.White.copy(alpha = .9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
        )
        Text("${vm.stopName(stop)}, ${p.name}", color = Color.White, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
        Text(line, color = Color.White.copy(alpha = .92f), fontSize = 15.sp)
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(p.currency)
            Chip("Plugs " + p.plugs.joinToString(" / "))
            Chip(if (p.voltage == Home.VOLTAGE) "230V" else "${p.voltage}V")
        }
    }
}

@Composable
private fun Chip(t: String) {
    Text(
        t, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(Color.White.copy(alpha = .22f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = .4f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
