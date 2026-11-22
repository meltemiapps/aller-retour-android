@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.allerretour.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.Home
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Flag
import io.github.allerretour.ui.InfoCard
import io.github.allerretour.ui.SectionTitle
import io.github.allerretour.ui.Small
import io.github.allerretour.ui.Tone

private val categories = listOf("Food", "Transport", "Stay", "Activities", "Shopping", "Other")

@Composable
fun MoneyScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val pack = vm.activePack(s)
    val rate = vm.rate(s, pack)
    var amount by remember { mutableStateOf("20") }
    var toLocal by remember { mutableStateOf(true) }
    var editRate by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf("") }
    var spend by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Food") }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("Converter")
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        amount, { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (toLocal) Home.CURRENCY else pack.currency) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { toLocal = !toLocal }) { Text("⇄", fontSize = 22.sp) }
                }
                val a = amount.toDoubleOrNull() ?: 0.0
                val out = if (toLocal) a * rate else a / rate
                Text(
                    if (toLocal) TripLogic.money(out, pack.currencySymbol, pack.currency) else TripLogic.money(out, Home.SYMBOL, Home.CURRENCY),
                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Small("£1 = ${TripLogic.money(rate, pack.currencySymbol, pack.currency)} · " + if (vm.rateIsUserSet(s, pack)) "your rate" else "indicative rate", Modifier.weight(1f))
                    TextButton(onClick = { editRate = true }) { Text("Set rate") }
                }
                if (!vm.rateIsUserSet(s, pack)) Flag("The app works offline and never fetches rates. Check today's rate from your bank or card app and set it here.", Tone.WARN)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quick = if (toLocal) listOf(10, 20, 50, 100) else when (pack.currency) { "JPY" -> listOf(1000, 3000, 5000, 10000); "THB" -> listOf(100, 500, 1000, 2000); else -> listOf(5, 10, 20, 50) }
                    quick.forEach { q -> FilterChip(selected = false, onClick = { amount = q.toString() }, label = { Text((if (toLocal) Home.SYMBOL else pack.currencySymbol) + q) }) }
                }
            }
        }
        item {
            SectionTitle("Spending")
            InfoCard {
                OutlinedTextField(desc, { desc = it }, label = { Text("What for?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    spend, { spend = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount in ${pack.currency}") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
                }
                Button(
                    onClick = { spend.toDoubleOrNull()?.takeIf { it > 0 }?.let { vm.addExpense(desc, it, cat); desc = ""; spend = "" } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add expense in ${pack.currency}") }

                if (s.expenses.isEmpty()) Small("No spending logged yet.", Modifier.padding(top = 8.dp))
                s.expenses.forEach { e ->
                    val p = vm.packs[e.country]
                    val stop = s.stops.firstOrNull { it.id == e.stopId }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.description, fontWeight = FontWeight.SemiBold)
                            Small("${p.flag} ${vm.stopName(stop)} · ${e.category}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(TripLogic.money(e.amount, p.currencySymbol, p.currency), fontWeight = FontWeight.Bold)
                            Small("≈ " + TripLogic.money(e.amount / vm.rate(s, p), Home.SYMBOL, Home.CURRENCY))
                        }
                        TextButton(onClick = { vm.removeExpense(e.id) }) { Text("✕") }
                    }
                }
                if (s.expenses.isNotEmpty()) {
                    val totalGbp = s.expenses.sumOf { it.amount / vm.rate(s, vm.packs[it.country]) }
                    val byCurrency = s.expenses.groupBy { vm.packs[it.country].currency }.map { (cur, list) ->
                        val p = vm.packs[list.first().country]
                        TripLogic.money(list.sumOf { it.amount }, p.currencySymbol, cur)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row {
                        Text("Total", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(TripLogic.money(totalGbp, Home.SYMBOL, Home.CURRENCY), fontWeight = FontWeight.ExtraBold)
                            Small(byCurrency.joinToString(" + "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (editRate) {
        var text by remember { mutableStateOf(if (vm.rateIsUserSet(s, pack)) rate.toString() else "") }
        AlertDialog(
            onDismissRequest = { editRate = false },
            title = { Text("Set your ${pack.currency} rate") },
            text = {
                Column {
                    Small("How many ${pack.currency} you get for £1 today. Your bank or card app shows the current rate.")
                    OutlinedTextField(
                        text, { text = it.filter { c -> c.isDigit() || c == '.' } }, singleLine = true,
                        label = { Text("${pack.currency} per £1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Small("Leave blank to go back to the indicative rate (${pack.indicativeRatePerGbp}).", Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = { FilledTonalButton(onClick = { vm.setRate(pack.currency, text.toDoubleOrNull()); editRate = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editRate = false }) { Text("Cancel") } },
        )
    }
}
