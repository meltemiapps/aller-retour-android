@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package io.github.allerretour.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.allerretour.AppViewModel
import io.github.allerretour.data.Expense
import io.github.allerretour.data.Home
import io.github.allerretour.logic.TripLogic
import io.github.allerretour.ui.Brand
import io.github.allerretour.ui.Field
import io.github.allerretour.ui.Footnote
import io.github.allerretour.ui.GlassButton
import io.github.allerretour.ui.GlassCard
import io.github.allerretour.ui.MenuPicker
import io.github.allerretour.ui.Screen
import io.github.allerretour.ui.Sub
import io.github.allerretour.ui.accent
import io.github.allerretour.ui.secondary
import java.util.Locale

private val categories = listOf("Food", "Transport", "Stay", "Sights", "Shopping", "Other")

private fun fmt(v: Double) = if (v == Math.rint(v)) v.toLong().toString() else String.format(Locale.UK, "%.2f", v)
private fun parse(t: String) = t.replace(',', '.').toDoubleOrNull()

@Composable
fun MoneyScreen(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var amountText by rememberSaveable { mutableStateOf("20") }
    var toLocal by rememberSaveable { mutableStateOf(true) }
    var desc by rememberSaveable { mutableStateOf("") }
    var spendText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Food") }

    Screen("Money") {
        val stop = vm.activeStop(s)
        if (stop == null) {
            EmptyTrip(vm)
            return@Screen
        }
        val p = vm.activePack(s)
        val r = vm.rate(s, p)
        val amount = parse(amountText) ?: 0.0

        GlassCard(title = "Converter") {
            Text(if (toLocal) "£${fmt(amount)} in ${p.currency}" else "${p.currencySymbol}${fmt(amount)} in pounds", color = secondary, fontSize = 16.sp)
            AnimatedContent(
                if (toLocal) TripLogic.money(amount * r, p.currencySymbol, p.currency) else TripLogic.money(amount / r, Home.SYMBOL, Home.CURRENCY),
                label = "converted",
            ) { Text(it, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold, color = accent) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10.0, 20.0, 50.0, 100.0).forEach { v ->
                    GlassButton((if (toLocal) "£" else p.currencySymbol) + fmt(v), prominent = amount == v) { amountText = fmt(v) }
                }
            }
            Field(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Amount", keyboard = KeyboardType.Decimal)
            GlassButton("Swap currencies", icon = Icons.Outlined.SwapHoriz) { toLocal = !toLocal }
        }

        GlassCard(title = "Your rate") {
            var rateText by remember(p.currency) { mutableStateOf(String.format(Locale.UK, "%.4f", r).trimEnd('0').let { if (it.substringAfter('.').length < 2) String.format(Locale.UK, "%.2f", r) else it }) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("£1 =", fontSize = 16.sp)
                Field(
                    rateText, { t -> rateText = t.filter { c -> c.isDigit() || c == '.' || c == ',' }; vm.setRate(p.currency, parse(rateText)) },
                    "Rate", keyboard = KeyboardType.Decimal, modifier = Modifier.padding(horizontal = 8.dp).width(120.dp),
                )
                Text(p.currency, fontSize = 16.sp)
            }
            Footnote("Set the rate your bank or card gives you. The app never goes online, so it never fetches rates.")
        }

        GlassCard(title = "Add an expense") {
            Field(desc, { desc = it }, "What was it?")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field(spendText, { spendText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Amount in ${p.currency}", keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                MenuPicker(categories, category, { it }, Modifier.padding(start = 8.dp)) { category = it }
            }
            val spend = parse(spendText) ?: 0.0
            GlassButton("Add", prominent = true, enabled = spend > 0) {
                if (spend > 0) { vm.addExpense(desc, spend, category); desc = ""; spendText = "" }
            }
        }

        Spent(vm)
    }
}

@Composable
private fun Spent(vm: AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val inGbp = { e: Expense -> e.amount / vm.rate(s, vm.packs[e.country]) }
    val total = s.expenses.sumOf(inGbp)
    GlassCard(title = "Spent so far", trailing = TripLogic.money(total, Home.SYMBOL, Home.CURRENCY)) {
        if (s.expenses.isEmpty()) {
            Text("Nothing yet. Expenses are converted to pounds at your rate.", color = secondary, fontSize = 16.sp)
        } else {
            val byCat = s.expenses.groupBy { it.category }.mapValues { (_, l) -> l.sumOf(inGbp) }.entries.sortedByDescending { it.value }
            Sub(byCat.joinToString(" · ") { "${it.key} ${TripLogic.money(it.value, Home.SYMBOL, Home.CURRENCY)}" })
            // Newest first; long-press for Delete, like the iOS context menu.
            s.expenses.take(8).forEach { e ->
                var menu by remember(e.id) { mutableStateOf(false) }
                val p = vm.packs[e.country]
                Box {
                    Row(Modifier.fillMaxWidth().combinedClickable(onLongClick = { menu = true }, onClick = {}).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(e.description, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(TripLogic.money(e.amount, p.currencySymbol, p.currency), fontSize = 16.sp, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Delete", color = Brand.Danger) }, onClick = { vm.removeExpense(e.id); menu = false })
                    }
                }
            }
        }
    }
}
