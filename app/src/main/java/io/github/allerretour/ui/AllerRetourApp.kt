@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.allerretour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.allerretour.AppViewModel
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.screens.GuideScreen
import io.github.allerretour.ui.screens.KitScreen
import io.github.allerretour.ui.screens.MoneyScreen
import io.github.allerretour.ui.screens.PlanScreen
import io.github.allerretour.ui.screens.SafetyScreen
import io.github.allerretour.ui.screens.TodayScreen
import java.time.Instant
import kotlin.math.roundToInt

private data class Tab(val label: String, val icon: String)
private val tabs = listOf(Tab("Today", "🕑"), Tab("Plan", "🗺"), Tab("Money", "💷"), Tab("Safety", "🛡"), Tab("Guide", "📖"), Tab("Kit", "🎒"))

@Composable
fun AllerRetourApp(vm: AppViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    if (!s.onboarded || s.stops.isEmpty()) {
        Onboarding(vm)
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TripHeader(vm) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(t.icon, fontSize = 20.sp) },
                        label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> TodayScreen(vm)
                1 -> PlanScreen(vm)
                2 -> MoneyScreen(vm)
                3 -> SafetyScreen(vm)
                4 -> GuideScreen(vm)
                else -> KitScreen(vm)
            }
        }
    }
}

@Composable
private fun TripHeader(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val pack = vm.activePack(s)
    var open by remember { mutableStateOf(false) }
    val diff = TripLogic.hoursVsUk(pack.timeZone, Instant.now())
    val diffText = when {
        diff == 0.0 -> "Same time as UK"
        diff % 1.0 == 0.0 -> (if (diff > 0) "+" else "") + diff.roundToInt() + "h vs UK"
        else -> (if (diff > 0) "+" else "") + diff + "h vs UK"
    }
    Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimaryContainer)), RoundedCornerShape(24.dp))
                .clickable { open = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("STOP ${s.activeIndex + 1} OF ${s.stops.size} · tap to switch", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${vm.stopName(vm.activeStop(s))}, ${pack.name}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(pack.flag, fontSize = 28.sp)
            }
            Text("${pack.currency} · Plugs ${pack.plugs.joinToString("/")} · $diffText", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .85f), fontSize = 12.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            s.stops.forEachIndexed { i, st ->
                val p = vm.packs[st.country]
                DropdownMenuItem(
                    text = { Text("${i + 1}. ${p.flag} ${vm.stopName(st)} · ${TripLogic.pretty(st.from)}") },
                    onClick = { vm.setActive(i); open = false },
                )
            }
        }
    }
}

/** First run: privacy promise and the first stop. */
@Composable
private fun Onboarding(vm: AppViewModel) {
    val context = LocalContext.current
    var country by remember { mutableStateOf("GR") }
    var city by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text("🧭", fontSize = 48.sp)
        Text("Aller-Retour", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text("Your travel companion that stays in your pocket.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(14.dp))
        InfoCard {
            Text("Our promise", fontWeight = FontWeight.ExtraBold)
            Bullet("No account, no sign-up")
            Bullet("No internet access: the app cannot send anything anywhere")
            Bullet("No ads, no trackers, no analytics")
            Bullet("Everything stays on this phone. You choose when to export it")
            Bullet("Free and open source")
        }
        SectionTitle("Where are you going first?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.packs.packs.values.forEach { p ->
                FilterChip(selected = country == p.code, onClick = { country = p.code }, label = { Text("${p.flag} ${p.name}") })
            }
        }
        OutlinedTextField(city, { city = it }, label = { Text("City or area (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickDate(context, from) { from = it; if (to.isBlank() || to < it) to = it } }, modifier = Modifier.weight(1f)) {
                Text(if (from.isBlank()) "Arrive" else TripLogic.pretty(from))
            }
            OutlinedButton(onClick = { pickDate(context, to.ifBlank { from }) { to = it } }, modifier = Modifier.weight(1f)) {
                Text(if (to.isBlank()) "Leave" else TripLogic.pretty(to))
            }
        }
        Small("You can add up to 10 stops, in any mix of countries, from the Plan tab.", Modifier.padding(top = 6.dp))
        Button(onClick = { vm.startTrip(country, city, from, to) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Start my trip")
        }
    }
}
