package io.github.allerretour.data

import kotlinx.serialization.Serializable

// ---------- Country packs (read-only, bundled in assets/packs/*.json) ----------

@Serializable
data class EmergencyNumber(val label: String, val number: String, val primary: Boolean = false)

@Serializable
data class Phrase(val english: String, val local: String, val pronunciation: String = "")

@Serializable
data class MenuTerm(val term: String, val romanised: String = "", val meaning: String)

@Serializable
data class Driving(
    val side: String,
    val speedLimits: String,
    val mustCarry: String,
    val notes: List<String> = emptyList(),
)

@Serializable
data class SampleWeather(val tempC: Int, val icon: String)

@Serializable
data class CountryPack(
    val code: String,
    val name: String,
    val defaultCity: String,
    val flag: String,
    val timeZone: String,
    val currency: String,
    val currencySymbol: String,
    /** Indicative only. Users set their own rate in Money; the app never fetches rates. */
    val indicativeRatePerGbp: Double,
    val plugs: List<String>,
    val voltage: Int,
    val frequency: String,
    val exitCode: String,
    val callingCode: String,
    val lat: Double,
    val lon: Double,
    val eu: Boolean = false,
    val schengen: Boolean = false,
    val passportMonthsOnArrival: Int = 0,
    val emergency: List<EmergencyNumber>,
    val tipping: List<String> = emptyList(),
    val etiquette: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val water: String = "",
    val scams: List<String> = emptyList(),
    val language: String = "",
    val phrases: List<Phrase> = emptyList(),
    val menu: List<MenuTerm> = emptyList(),
    val driving: Driving,
    val idp: String = "",
    val medicines: String = "",
    val entry: List<String> = emptyList(),
    val sampleWeather: List<SampleWeather> = emptyList(),
)

/** Home country. Version 1 supports UK travellers; see ROADMAP.md. */
object Home {
    const val NAME = "United Kingdom"
    const val TIME_ZONE = "Europe/London"
    const val CALLING_CODE = "44"
    const val CURRENCY = "GBP"
    const val SYMBOL = "£"
    val PLUGS = listOf("G")
    const val VOLTAGE = 230
}

// ---------- User data (stored only in the app's private storage) ----------

@Serializable
data class Stop(
    val id: String,
    val country: String,
    val city: String = "",
    /** ISO dates, yyyy-MM-dd. Empty when not yet known. */
    val from: String = "",
    val to: String = "",
)

@Serializable
data class Expense(
    val id: String,
    val description: String,
    val amount: Double,
    val country: String,
    val stopId: String,
    val category: String = "Other",
    val createdAt: Long = 0L,
)

@Serializable
data class Pin(val label: String, val lat: Double, val lon: Double, val savedAt: Long)

@Serializable
data class AppState(
    val schemaVersion: Int = 1,
    val stops: List<Stop> = emptyList(),
    val activeIndex: Int = 0,
    val expenses: List<Expense> = emptyList(),
    /** User-set rates, units of local currency per £1, keyed by currency code. */
    val rates: Map<String, Double> = emptyMap(),
    val homeNumber: String = "",
    val readiness: Set<String> = emptySet(),
    val packTemplate: String = "Beach",
    val packed: Map<String, Set<Int>> = emptyMap(),
    val schengenDaysAlreadyUsed: Int = 0,
    val medicalNote: String = "",
    val passportExpiry: String = "",
    /** Optional dates for reminders, ISO yyyy-MM-dd. */
    val insuranceEnds: String = "",
    val ghicExpiry: String = "",
    /** Outbound flight, local date-time yyyy-MM-ddTHH:mm. */
    val flightDeparture: String = "",
    val medsTime: String = "08:00",
    val safeTime: String = "21:00",
    /** Reminder switches by [io.github.allerretour.logic.ReminderKind] name. Missing = the kind's default. */
    val reminders: Map<String, Boolean> = emptyMap(),
    val pin: Pin? = null,
    val onboarded: Boolean = false,
    /** Card style: "Clear" or "Tinted". Same names as the iPhone app. */
    val glass: String = "Clear",
    /** Background scene: "Sky", "Sunset", "Olive", "Harbour" or "Photo" (the user's own photo, kept in app storage). */
    val backdrop: String = "Sky",
)

const val MAX_STOPS = 10
