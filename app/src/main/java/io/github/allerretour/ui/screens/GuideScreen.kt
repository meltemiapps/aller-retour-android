@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.github.allerretour.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.dial
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.LocalContext
import io.github.allerretour.data.Home
import io.github.allerretour.data.Phrase
import io.github.allerretour.ui.BadBg
import io.github.allerretour.ui.BadFg
import io.github.allerretour.ui.Bullet
import io.github.allerretour.ui.InfoCard
import io.github.allerretour.ui.KeyValue
import io.github.allerretour.ui.SectionTitle
import io.github.allerretour.ui.ShowCard
import io.github.allerretour.ui.Small
import kotlin.math.abs

@Composable
fun GuideScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val tripCodes = TripLogic.tripCountries(s.stops)
    var code by remember { mutableStateOf<String?>(null) }
    val p = vm.packs[code?.takeIf { it in tripCodes } ?: vm.activePack(s).code]
    var section by remember { mutableStateOf("All") }
    fun show(vararg names: String) = section == "All" || section in names
    var shown by remember { mutableStateOf<Phrase?>(null) }
    val context = LocalContext.current
    var medical by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            if (tripCodes.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    tripCodes.forEach { c ->
                        val cp = vm.packs[c]
                        FilterChip(selected = p.code == c, onClick = { code = c; query = "" }, label = { Text("${cp.flag} ${cp.name}") })
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf("All", "Essentials", "Entry", "Money", "Customs", "Driving", "Health", "Phrases", "Menu").forEach { n ->
                    FilterChip(selected = section == n, onClick = { section = n }, label = { Text(n) })
                }
            }
        }
        if (show("Essentials")) item {
            SectionTitle("Emergency · ${p.name}")
            InfoCard {
                p.emergency.forEach { e ->
                    Row(Modifier.fillMaxWidth().clickable { dial(context, "tel:${e.number}") }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.label, Modifier.weight(1f))
                        Text(e.number, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (e.primary) BadFg else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (show("Entry")) item {
            SectionTitle("Entry & documents")
            InfoCard {
                Text(
                    if (p.schengen) "Schengen country: your passport must be under 10 years old on entry and valid for 3 months after you leave the Schengen area."
                    else if (p.passportMonthsOnArrival > 0) "Your passport needs at least ${p.passportMonthsOnArrival} months left on arrival."
                    else "Check the passport validity rules before you travel.",
                    fontWeight = FontWeight.SemiBold,
                )
                p.entry.forEach { Bullet(it) }
                if (p.idp.isNotBlank()) Bullet("International Driving Permit ${p.idp.lowercase()}")
            }
        }
        if (show("Money")) item {
            SectionTitle("Money & tipping")
            InfoCard {
                Text("${p.currency} · indicative £1 = ${p.currencySymbol}${"%.2f".format(p.indicativeRatePerGbp)}", fontWeight = FontWeight.Bold)
                p.tipping.forEach { Bullet(it) }
            }
        }
        if (show("Essentials")) item {
            SectionTitle("Power · ${p.name}")
            InfoCard {
                val needAdapter = "G" !in p.plugs
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.plugs.forEach { plug ->
                        Box(
                            Modifier.size(46.dp).background(if (needAdapter) BadBg else MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(plug, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if (needAdapter) BadFg else MaterialTheme.colorScheme.primary) }
                    }
                }
                Text("${p.voltage}V · ${p.frequency}Hz", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                Small("UK: ${Home.VOLTAGE}V · 50Hz, Type G")
                if (needAdapter) Bullet("UK plugs won't fit. Pack a Type ${p.plugs.joinToString("/")} adapter.", warn = true)
                if (abs(p.voltage - Home.VOLTAGE) > 20) Bullet("Lower voltage. Chargers marked \"100–240V\" are fine, but UK hair dryers, straighteners and kettles need a converter or a dual-voltage model.", warn = true)
                else Bullet("Same voltage as the UK, so an adapter is all you need.")
            }
        }
        if (show("Driving")) item {
            SectionTitle("Driving")
            InfoCard {
                KeyValue("Drive on the", p.driving.side)
                KeyValue("Speed limits", p.driving.speedLimits)
                KeyValue("Must carry", p.driving.mustCarry)
                p.driving.notes.forEach { Bullet(it) }
            }
        }
        if (show("Customs")) item {
            SectionTitle("Etiquette")
            InfoCard {
                p.etiquette.forEach { Bullet(it) }
                p.avoid.forEach { Bullet(it, warn = true) }
            }
        }
        if (show("Essentials", "Customs")) item {
            SectionTitle("Water & safety")
            InfoCard {
                Text("💧 ${p.water}", fontWeight = FontWeight.SemiBold)
                Small("COMMON SCAMS", Modifier.padding(top = 10.dp))
                p.scams.forEach { Bullet(it, warn = true) }
                Small("Save the nearest British embassy from GOV.UK before you travel.", Modifier.padding(top = 8.dp))
            }
        }
        if (show("Health")) item {
            SectionTitle("Medicines")
            InfoCard {
                Text(p.medicines, style = MaterialTheme.typography.bodyMedium)
                Small("Check the rules for every country on your route, including transit stops.", Modifier.padding(top = 6.dp))
            }
        }
        if (show("Phrases")) item {
            SectionTitle("Phrase cards · ${p.language}")
            InfoCard {
                if (p.phrases.isEmpty()) Small("English is spoken here, so no phrase pack is needed.")
                p.phrases.forEachIndexed { i, ph ->
                    if (i > 0) HorizontalDivider()
                    Row(Modifier.fillMaxWidth().clickable { shown = ph }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Small(ph.english)
                            Text(ph.local, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (ph.pronunciation.isNotBlank()) Text(ph.pronunciation, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                        Text("⤢", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        if (show("Menu")) item {
            SectionTitle("Menu decoder")
            InfoCard {
                OutlinedTextField(query, { query = it }, label = { Text("Type a word from the menu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val q = query.trim().lowercase()
                val hits = p.menu.filter { q.isEmpty() || "${it.term} ${it.romanised} ${it.meaning}".lowercase().contains(q) }
                if (hits.isEmpty()) Small("No match in the offline dictionary.", Modifier.padding(top = 6.dp))
                hits.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(m.term, fontWeight = FontWeight.Bold)
                            if (m.romanised.isNotBlank()) Small(m.romanised)
                        }
                        Small(m.meaning, Modifier.weight(1.3f))
                    }
                }
            }
        }
        if (show("Health")) item {
            SectionTitle("Medical card")
            InfoCard {
                var note by remember(s.medicalNote) { mutableStateOf(s.medicalNote) }
                OutlinedTextField(note, { note = it; vm.setMedicalNote(it) }, label = { Text("Allergies, conditions, medicines") }, modifier = Modifier.fillMaxWidth())
                FilledTonalButton(onClick = { medical = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Show in large text") }
                Small("Kept on this phone only. Shown with \"I need a doctor\" in the local language.", Modifier.padding(top = 6.dp))
            }
        }
    }

    shown?.let { ShowCard(it.english, it.local, it.pronunciation) { shown = null } }
    if (medical) {
        val doctor = p.phrases.firstOrNull { it.english == "I need a doctor" }?.local ?: "I need a doctor"
        ShowCard("Medical information", doctor, s.medicalNote.ifBlank { "Add your allergies and conditions in the Guide tab" }) { medical = false }
    }
}
