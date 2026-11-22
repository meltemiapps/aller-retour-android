package io.github.allerretour

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.allerretour.data.AppState
import io.github.allerretour.data.CountryPack
import io.github.allerretour.data.Expense
import io.github.allerretour.data.MAX_STOPS
import io.github.allerretour.data.PackRepository
import io.github.allerretour.data.Pin
import io.github.allerretour.data.StateStore
import io.github.allerretour.data.Stop
import io.github.allerretour.logic.TripLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val packs = PackRepository(app)
    private val store = StateStore(app)
    private val saveLock = Mutex()

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun change(f: (AppState) -> AppState) {
        _state.update(f)
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) { saveLock.withLock { store.save(snapshot) } }
    }

    private fun newId() = UUID.randomUUID().toString().take(8)

    fun activeStop(s: AppState): Stop? = s.stops.getOrNull(s.activeIndex.coerceIn(0, (s.stops.size - 1).coerceAtLeast(0)))
    fun activePack(s: AppState): CountryPack = packs[activeStop(s)?.country ?: "GR"]
    fun stopName(stop: Stop?): String = stop?.let { it.city.ifBlank { packs[it.country].defaultCity } } ?: "Home"

    /** Units of local currency per £1: the user's own rate if set, otherwise the bundled indicative rate. */
    fun rate(s: AppState, pack: CountryPack): Double = s.rates[pack.currency] ?: pack.indicativeRatePerGbp
    fun rateIsUserSet(s: AppState, pack: CountryPack) = s.rates.containsKey(pack.currency)

    // ---------- Trip ----------

    fun startTrip(country: String, city: String, from: String, to: String) = change {
        it.copy(stops = listOf(Stop(newId(), country, city.trim(), from, to)), activeIndex = 0, onboarded = true)
    }

    fun addStop(country: String, city: String, from: String, to: String) = change {
        if (it.stops.size >= MAX_STOPS) it
        else it.copy(stops = it.stops + Stop(newId(), country, city.trim(), from, to))
    }

    fun removeStop(index: Int) = change {
        if (it.stops.size <= 1) it else {
            val stops = it.stops.toMutableList().apply { removeAt(index) }
            val active = when {
                it.activeIndex > index -> it.activeIndex - 1
                it.activeIndex >= stops.size -> stops.size - 1
                else -> it.activeIndex
            }
            it.copy(stops = stops, activeIndex = active)
        }
    }

    fun moveStop(index: Int, by: Int) = change {
        val j = index + by
        if (j !in it.stops.indices) it else {
            val stops = it.stops.toMutableList()
            val tmp = stops[index]; stops[index] = stops[j]; stops[j] = tmp
            val active = when (it.activeIndex) { index -> j; j -> index; else -> it.activeIndex }
            it.copy(stops = stops, activeIndex = active)
        }
    }

    fun updateStop(index: Int, city: String, from: String, to: String, shiftLater: Boolean) = change {
        val dated = TripLogic.updateStopDates(it.stops, index, from, to, shiftLater)
        it.copy(stops = dated.mapIndexed { i, s -> if (i == index) s.copy(city = city.trim()) else s })
    }

    fun setActive(index: Int) = change { it.copy(activeIndex = index.coerceIn(0, it.stops.size - 1)) }

    // ---------- Money ----------

    fun setRate(currency: String, rate: Double?) = change {
        it.copy(rates = if (rate == null || rate <= 0) it.rates - currency else it.rates + (currency to rate))
    }

    fun addExpense(description: String, amount: Double, category: String) = change {
        val stop = activeStop(it) ?: return@change it
        it.copy(expenses = listOf(Expense(newId(), description.ifBlank { "Expense" }, amount, stop.country, stop.id, category, System.currentTimeMillis())) + it.expenses)
    }

    fun removeExpense(id: String) = change { it.copy(expenses = it.expenses.filterNot { e -> e.id == id }) }

    // ---------- Checklists & settings ----------

    fun setHomeNumber(n: String) = change { it.copy(homeNumber = n) }
    fun toggleReady(key: String) = change { it.copy(readiness = if (key in it.readiness) it.readiness - key else it.readiness + key) }
    fun setPackTemplate(t: String) = change { it.copy(packTemplate = t) }
    fun togglePacked(i: Int) = change {
        val cur = it.packed[it.packTemplate].orEmpty()
        it.copy(packed = it.packed + (it.packTemplate to (if (i in cur) cur - i else cur + i)))
    }
    fun resetPacked() = change { it.copy(packed = it.packed - it.packTemplate) }
    fun setSchengenUsed(n: Int) = change { it.copy(schengenDaysAlreadyUsed = n.coerceIn(0, 180)) }
    fun setMedicalNote(t: String) = change { it.copy(medicalNote = t) }
    fun setPassportExpiry(d: String) = change { it.copy(passportExpiry = d) }
    fun savePin(label: String, lat: Double, lon: Double) = change { it.copy(pin = Pin(label, lat, lon, System.currentTimeMillis())) }
    fun clearPin() = change { it.copy(pin = null) }
    fun setWallpaperColours(on: Boolean) = change { it.copy(useWallpaperColours = on) }

    // ---------- Backup ----------

    fun exportJson(): String = store.export(_state.value)

    fun importJson(text: String): Boolean = runCatching {
        val imported = store.import(text)
        change { imported.copy(onboarded = imported.stops.isNotEmpty()) }
    }.isSuccess

    fun eraseEverything() = change { AppState() }
}
