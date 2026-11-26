package io.github.allerretour

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.allerretour.logic.ReminderKind
import io.github.allerretour.notify.ReminderScheduler
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
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max

class AppViewModel(app: Application) : AndroidViewModel(app) {
    init { runCatching { ReminderScheduler.ensureChannel(app) } }

    val packs = PackRepository(app)
    private val store = StateStore(app)
    private val saveLock = Mutex()

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private fun change(f: (AppState) -> AppState) {
        _state.update(f)
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) { saveLock.withLock { store.save(snapshot) } }
        runCatching { ReminderScheduler.reschedule(getApplication(), snapshot, packs.packs) }
    }

    // ---------- Reminders (local notifications only) ----------
    fun setReminder(kind: ReminderKind, on: Boolean) = change { it.copy(reminders = it.reminders + (kind.name to on)) }
    fun setFlight(isoLocalDateTime: String) = change { it.copy(flightDeparture = isoLocalDateTime) }
    fun setInsuranceEnds(d: String) = change { it.copy(insuranceEnds = d) }
    fun setGhicExpiry(d: String) = change { it.copy(ghicExpiry = d) }
    fun setMedsTime(t: String) = change { it.copy(medsTime = t) }
    fun setSafeTime(t: String) = change { it.copy(safeTime = t) }

    private fun newId() = UUID.randomUUID().toString().take(8)

    // ---------- Screen state shared across tabs (like AppStore.tab and showAbout on iOS) ----------
    var tab by mutableStateOf(0)
    var showAbout by mutableStateOf(false)
    /** Phrase shown full screen in large text (fullScreenCover on iOS). */
    var phrase by mutableStateOf<io.github.allerretour.data.Phrase?>(null)

    /** Today's stop when the trip is under way, otherwise the saved one. Same rule as the iPhone app. */
    fun activeIndex(s: AppState): Int {
        val today = TripLogic.stopForDate(s.stops, LocalDate.now())
        return if (today >= 0) today else s.activeIndex.coerceIn(0, (s.stops.size - 1).coerceAtLeast(0))
    }
    fun activeStop(s: AppState): Stop? = s.stops.getOrNull(activeIndex(s))
    fun activePack(s: AppState): CountryPack = packs[activeStop(s)?.country ?: "GR"]
    fun stopName(stop: Stop?): String = stop?.let { it.city.ifBlank { packs[it.country].defaultCity } } ?: "Home"

    /** Units of local currency per £1: the user's own rate if set, otherwise the bundled indicative rate. */
    fun rate(s: AppState, pack: CountryPack): Double = s.rates[pack.currency] ?: pack.indicativeRatePerGbp
    fun rateIsUserSet(s: AppState, pack: CountryPack) = s.rates.containsKey(pack.currency)

    // ---------- Trip ----------

    fun addStop(country: String, city: String, from: String, to: String) = change {
        if (it.stops.size >= MAX_STOPS) it
        else it.copy(stops = it.stops + Stop(newId(), country, city.trim().ifBlank { packs[country].defaultCity }, from, to), onboarded = true)
    }

    /** One night more or fewer at a stop. Later stops move with it. */
    fun extendStay(index: Int, by: Long) = change {
        val st = it.stops.getOrNull(index) ?: return@change it
        val t = TripLogic.date(st.to) ?: return@change it
        val nt = t.plusDays(by)
        val f = TripLogic.date(st.from)
        if (f != null && nt.isBefore(f)) it
        else it.copy(stops = TripLogic.updateStopDates(it.stops, index, st.from, nt.toString(), shiftLater = true))
    }

    fun removeStop(index: Int) = change {
        if (index !in it.stops.indices) it else {
            val stops = it.stops.toMutableList().apply { removeAt(index) }
            val active = when {
                it.activeIndex > index -> it.activeIndex - 1
                it.activeIndex >= stops.size -> (stops.size - 1).coerceAtLeast(0)
                else -> it.activeIndex
            }
            it.copy(stops = stops, activeIndex = active)
        }
    }

    fun updateStop(index: Int, country: String, city: String, from: String, to: String, shiftLater: Boolean) = change {
        val dated = TripLogic.updateStopDates(it.stops, index, from, to, shiftLater)
        it.copy(stops = dated.mapIndexed { i, s -> if (i == index) s.copy(country = country, city = city.trim()) else s })
    }

    // ---------- Money ----------

    fun setRate(currency: String, rate: Double?) = change {
        it.copy(rates = if (rate == null || rate <= 0) it.rates - currency else it.rates + (currency to rate))
    }

    fun addExpense(description: String, amount: Double, category: String) = change {
        val stop = activeStop(it) ?: return@change it
        it.copy(expenses = listOf(Expense(newId(), description.ifBlank { category }, amount, stop.country, stop.id, category, System.currentTimeMillis())) + it.expenses)
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
    fun setSchengenUsed(n: Int) = change { it.copy(schengenDaysAlreadyUsed = n.coerceIn(0, 90)) }
    fun setMedicalNote(t: String) = change { it.copy(medicalNote = t) }
    fun setPassportExpiry(d: String) = change { it.copy(passportExpiry = d) }
    fun savePin(label: String, lat: Double, lon: Double) = change { it.copy(pin = Pin(label, lat, lon, System.currentTimeMillis())) }
    fun clearPin() = change { it.copy(pin = null) }
    fun setGlass(g: String) = change { it.copy(glass = g) }
    fun setBackdrop(b: String) = change { it.copy(backdrop = b) }

    // ---------- Background photo (copied into app storage, never leaves the phone) ----------

    private val photoFile = File(app.filesDir, "backdrop.jpg")
    private val _photo = MutableStateFlow<ImageBitmap?>(null)
    val photo: StateFlow<ImageBitmap?> = _photo.asStateFlow()
    val hasPhoto: Boolean get() = photoFile.exists()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (photoFile.exists()) _photo.value = runCatching { BitmapFactory.decodeFile(photoFile.path)?.asImageBitmap() }.getOrNull()
        }
    }

    /** Copies the picked photo into private storage, scaled down to at most 2400 px, then uses it. */
    fun savePhoto(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val ok = runCatching {
            val ctx = getApplication<Application>()
            val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val big = max(info.size.width, info.size.height)
                    if (big > 2400) { val f = 2400.0 / big; d.setTargetSize((info.size.width * f).toInt(), (info.size.height * f).toInt()) }
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2400) sample *= 2
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                    ?: error("unreadable")
            }
            val tmp = File(photoFile.parentFile, "backdrop.tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (!tmp.renameTo(photoFile)) { tmp.copyTo(photoFile, overwrite = true); tmp.delete() }
            _photo.value = bmp.asImageBitmap()
        }.isSuccess
        withContext(Dispatchers.Main) {
            if (ok) setBackdrop("Photo")
            onDone(ok)
        }
    }

    // ---------- Backup ----------

    fun exportJson(): String = store.export(_state.value)

    fun importJson(text: String): Boolean = runCatching {
        val imported = store.import(text)
        change { imported.copy(onboarded = imported.stops.isNotEmpty()) }
    }.isSuccess

    fun eraseEverything() {
        runCatching { photoFile.delete() }
        _photo.value = null
        change { AppState() }
    }
}
