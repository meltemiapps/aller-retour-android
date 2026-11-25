package io.github.allerretour.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.Home
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.logic.ReminderPlan
import io.github.allerretour.ui.Flag
import io.github.allerretour.ui.InfoCard
import io.github.allerretour.ui.KeyValue
import io.github.allerretour.ui.SectionTitle
import io.github.allerretour.ui.Small
import io.github.allerretour.ui.Tone
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val hhmm = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

@Composable
fun TodayScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val pack = vm.activePack(s)
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (true) { now = Instant.now(); delay(15_000) } }

    val zone = ZoneId.of(pack.timeZone)
    val today = now.atZone(zone).toLocalDate()
    val start = TripLogic.tripStart(s.stops)
    val end = TripLogic.tripEnd(s.stops)
    val readyItems = TripLogic.readinessItems(s.stops, vm.packs.packs)
    val readyDone = readyItems.count { it.first in s.readiness }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("Daily brief")
            InfoCard {
                Text(today.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                when {
                    start != null && today.isBefore(start) -> {
                        val days = ChronoUnit.DAYS.between(today, start)
                        Text("Trip starts in $days day${if (days == 1L) "" else "s"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                        LinearProgressIndicator(progress = { if (readyItems.isEmpty()) 0f else readyDone / readyItems.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                        Small("$readyDone of ${readyItems.size} pre-departure checks done (Kit tab)")
                    }
                    end != null && today.isAfter(end) -> Flag("Trip complete. Welcome home!", Tone.GOOD)
                    else -> {
                        val i = TripLogic.stopForDate(s.stops, today)
                        val here = s.stops.getOrNull(i)
                        if (here != null) {
                            KeyValue("You are in", "${vm.packs[here.country].flag} ${vm.stopName(here)}")
                            if (here.to == today.toString()) Flag("Moving on today. Check your onward travel", Tone.WARN)
                            val next = s.stops.getOrNull(i + 1)
                            if (next != null) KeyValue("Next stop", "${vm.stopName(next)} · ${TripLogic.pretty(next.from)}")
                            if (i != s.activeIndex) Small("The header is showing a different stop. Tap it to switch.")
                        } else Small("No stop planned for today.")
                    }
                }
                TripLogic.sunTimes(pack.lat, pack.lon, today)?.let { (rise, set) ->
                    KeyValue("Sunrise · sunset (${vm.stopName(vm.activeStop(s))})", "${rise.atZone(zone).format(hhmm)} · ${set.atZone(zone).format(hhmm)}")
                }
            }
        }
        item {
            val upcoming = ReminderPlan.upcoming(s, vm.packs.packs)
            SectionTitle("Coming up")
            InfoCard {
                if (upcoming.isEmpty()) Small("No reminders scheduled. Turn some on in Kit.")
                upcoming.forEach { r ->
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(r.kind.title, fontWeight = FontWeight.Bold)
                        Small(ReminderPlan.whenLine(r))
                    }
                }
            }
        }
        item {
            SectionTitle("Time")
            InfoCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Clock("Local · ${vm.stopName(vm.activeStop(s))}", now.atZone(zone).format(hhmm))
                    Clock("Home · UK", now.atZone(ZoneId.of(Home.TIME_ZONE)).format(hhmm))
                }
                val ukTime = now.atZone(ZoneId.of(Home.TIME_ZONE)).format(hhmm)
                when (TripLogic.callTime(now)) {
                    TripLogic.CallTime.GOOD -> Flag("✓ Good time to call home ($ukTime in the UK)", Tone.GOOD)
                    TripLogic.CallTime.BORDERLINE -> Flag("Borderline: it's $ukTime in the UK", Tone.WARN)
                    TripLogic.CallTime.LATE -> Flag("It's $ukTime in the UK. They may be asleep", Tone.BAD)
                }
            }
        }
        if (TripLogic.hasSchengen(s.stops, vm.packs.packs)) item {
            val used = TripLogic.schengenMaxDays(s.stops, vm.packs.packs, s.schengenDaysAlreadyUsed)
            SectionTitle("Schengen 90/180")
            InfoCard {
                KeyValue("Days used (highest in any 180-day window)", "$used / 90")
                LinearProgressIndicator(progress = { (used / 90f).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth())
                if (used > 90) Flag("Over the limit by ${used - 90} days. Shorten the Schengen part of your trip", Tone.BAD)
            }
        }
    }
}

@Composable
private fun Clock(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Small(label)
        Text(time, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    }
}
