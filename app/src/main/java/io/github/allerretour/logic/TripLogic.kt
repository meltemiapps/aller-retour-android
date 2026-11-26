package io.github.allerretour.logic

import io.github.allerretour.data.CountryPack
import io.github.allerretour.data.Home
import io.github.allerretour.data.Stop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure functions with no Android dependencies, so they are easy to unit-test. */
object TripLogic {

    fun date(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

    private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    fun pretty(s: String): String = date(s)?.format(shortDate) ?: "Set date"

    fun nights(stop: Stop): Int {
        val f = date(stop.from) ?: return 0
        val t = date(stop.to) ?: return 0
        return ChronoUnit.DAYS.between(f, t).toInt().coerceAtLeast(0)
    }

    fun tripStart(stops: List<Stop>): LocalDate? = stops.mapNotNull { date(it.from) }.minOrNull()
    fun tripEnd(stops: List<Stop>): LocalDate? = stops.mapNotNull { date(it.to) }.maxOrNull()

    /** Index of the stop you are in on [day], or -1. */
    fun stopForDate(stops: List<Stop>, day: LocalDate): Int {
        stops.forEachIndexed { i, s ->
            val f = date(s.from); val t = date(s.to)
            if (f != null && t != null && !day.isBefore(f) && day.isBefore(t)) return i
        }
        return stops.indexOfFirst { date(it.to) == day }
    }

    /**
     * Change one stop's dates. When [shiftLater] is true, every later stop moves by the
     * same number of days as the leave date changed ("extend my stay").
     */
    fun updateStopDates(stops: List<Stop>, index: Int, from: String, to: String, shiftLater: Boolean): List<Stop> {
        val old = stops[index]
        val delta = run {
            val a = date(old.to); val b = date(to)
            if (a != null && b != null) ChronoUnit.DAYS.between(a, b) else 0L
        }
        return stops.mapIndexed { i, s ->
            when {
                i == index -> s.copy(from = from, to = to)
                i > index && shiftLater && delta != 0L -> s.copy(
                    from = date(s.from)?.plusDays(delta)?.toString() ?: s.from,
                    to = date(s.to)?.plusDays(delta)?.toString() ?: s.to,
                )
                else -> s
            }
        }
    }

    /** Warnings about gaps and overlaps between consecutive stops. */
    fun routeWarnings(stops: List<Stop>): Map<Int, String> {
        val out = mutableMapOf<Int, String>()
        for (i in 0 until stops.size - 1) {
            val t = date(stops[i].to) ?: continue
            val nf = date(stops[i + 1].from) ?: continue
            val gap = ChronoUnit.DAYS.between(t, nf)
            if (gap < 0) out[i] = "Overlaps the next stop by ${-gap} day(s)"
            else if (gap > 1) out[i] = "${gap - 1} unplanned night(s) before the next stop"
        }
        return out
    }

    /**
     * Highest number of days spent in the Schengen area in any rolling 180-day window,
     * counting arrival and departure days. [alreadyUsed] covers earlier trips.
     */
    fun schengenMaxDays(stops: List<Stop>, packs: Map<String, CountryPack>, alreadyUsed: Int): Int {
        val days = sortedSetOf<LocalDate>()
        stops.filter { packs[it.country]?.schengen == true }.forEach { s ->
            var d = date(s.from) ?: return@forEach
            val t = date(s.to) ?: return@forEach
            while (!d.isAfter(t)) { days += d; d = d.plusDays(1) }
        }
        if (days.isEmpty()) return 0
        val list = days.toList()
        return list.maxOf { day ->
            val lo = day.minusDays(179)
            list.count { !it.isBefore(lo) && !it.isAfter(day) }
        } + alreadyUsed
    }

    fun hasSchengen(stops: List<Stop>, packs: Map<String, CountryPack>) = stops.any { packs[it.country]?.schengen == true }

    fun tripCountries(stops: List<Stop>): List<String> = stops.map { it.country }.distinct()

    fun tripPlugs(stops: List<Stop>, packs: Map<String, CountryPack>): List<String> =
        stops.flatMap { packs[it.country]?.plugs.orEmpty() }.distinct().sorted()

    // ---------- Calling home ----------

    data class Dial(val international: String, val fromHere: String, val telUri: String)

    /** Formats a UK number for dialling from abroad. Returns null when the input has no digits. */
    fun ukDial(raw: String, exitCode: String): Dial? {
        val cleaned = raw.filter { it.isDigit() || it == '+' }
        if (cleaned.none { it.isDigit() }) return null
        val national = cleaned.removePrefix("+44").removePrefix("0044").removePrefix("0")
        val pretty = if (national.length >= 10) "${national.substring(0, 4)} ${national.substring(4, 7)} ${national.substring(7)}" else national
        return Dial("+44 $pretty", "$exitCode 44 $national", "tel:+44$national")
    }

    enum class CallTime { GOOD, BORDERLINE, LATE }

    fun callTime(now: Instant): CallTime {
        val h = now.atZone(ZoneId.of(Home.TIME_ZONE)).hour
        return when {
            h in 9..20 -> CallTime.GOOD
            h in 7..22 -> CallTime.BORDERLINE
            else -> CallTime.LATE
        }
    }

    /** Hours the destination is ahead of the UK right now (may be negative or .5). */
    fun hoursVsUk(zone: String, now: Instant): Double {
        val a = ZoneId.of(zone).rules.getOffset(now).totalSeconds
        val b = ZoneId.of(Home.TIME_ZONE).rules.getOffset(now).totalSeconds
        return (a - b) / 3600.0
    }

    /** "Same time as UK", "+2h vs UK", "−5.5h vs UK", as on iOS. */
    fun hoursLabel(h: Double): String {
        if (h == 0.0) return "Same time as UK"
        val a = kotlin.math.abs(h)
        val v = if (a % 1.0 == 0.0) a.toInt().toString() else String.format(Locale.UK, "%.1f", a)
        return if (h > 0) "+${v}h vs UK" else "\u2212${v}h vs UK"
    }

    // ---------- Money ----------

    fun money(amount: Double, symbol: String, currency: String): String {
        val dp = if (currency == "JPY") 0 else 2
        return symbol + String.format(Locale.UK, "%,.${dp}f", amount)
    }

    // ---------- Location ----------

    fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dp = Math.toRadians(lat2 - lat1); val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2); val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun compassPoint(deg: Double): String = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((deg / 45).roundToInt()) % 8]

    fun distanceLabel(m: Double): String = if (m >= 1000) String.format(Locale.UK, "%.2f km", m / 1000) else "${m.roundToInt()} m"

    /**
     * Sunrise and sunset for a place and date, calculated on the device (standard
     * sunrise equation, accurate to a minute or two). Null in polar day/night.
     */
    fun sunTimes(lat: Double, lon: Double, day: LocalDate): Pair<Instant, Instant>? {
        val rad = Math.PI / 180
        val jd = day.toEpochDay() + 0.5 + 2440587.5
        val n = kotlin.math.floor(jd - 2451545.0 + 0.0008)
        val jStar = n - lon / 360
        val m = (357.5291 + 0.98560028 * jStar) % 360
        val c = 1.9148 * sin(m * rad) + 0.02 * sin(2 * m * rad) + 0.0003 * sin(3 * m * rad)
        val l = (m + c + 180 + 102.9372) % 360
        val jTransit = 2451545.0 + jStar + 0.0053 * sin(m * rad) - 0.0069 * sin(2 * l * rad)
        val sinD = sin(l * rad) * sin(23.4397 * rad)
        val cosW = (sin(-0.833 * rad) - sin(lat * rad) * sinD) / (cos(lat * rad) * cos(asin(sinD)))
        if (cosW < -1 || cosW > 1) return null
        val w = Math.toDegrees(kotlin.math.acos(cosW))
        fun toInstant(j: Double) = Instant.ofEpochMilli(((j - 2440587.5) * 86_400_000).toLong())
        return toInstant(jTransit - w / 360) to toInstant(jTransit + w / 360)
    }

    // ---------- Checklists ----------

    val packingTemplates: Map<String, List<String>> = linkedMapOf(
        "Beach" to listOf("Passport", "Travel insurance documents", "Plug adapters", "Phone charger & power bank", "Sunscreen SPF 30+", "Sunglasses", "Swimwear", "Flip-flops", "Hat", "Basic first-aid kit", "Prescription medicines"),
        "City" to listOf("Passport", "Travel insurance documents", "Plug adapters", "Phone charger & power bank", "Comfortable walking shoes", "Light rain jacket", "Day bag", "Printed copy of bookings", "Prescription medicines"),
        "Winter" to listOf("Passport", "Travel insurance documents", "Plug adapters", "Thermal layers", "Waterproof jacket", "Gloves & hat", "Lip balm", "Phone charger & power bank", "Prescription medicines"),
        "Business" to listOf("Passport", "Laptop & charger", "Plug adapters", "Business cards", "Presentation saved offline", "Smart outfit", "Comfortable shoes", "Travel insurance documents", "Prescription medicines"),
    )

    fun packingList(template: String, stops: List<Stop>, packs: Map<String, CountryPack>): List<String> {
        val countries = tripCountries(stops).mapNotNull { packs[it] }
        val base = packingTemplates[template].orEmpty().map {
            if (it == "Plug adapters") "Plug adapters (Type ${tripPlugs(stops, packs).joinToString("/")})" else it
        }
        val extra = buildList {
            if (countries.any { it.voltage < 200 }) add("Voltage converter (110V stop on route)")
            if (countries.any { it.idp.isNotBlank() }) add("International Driving Permit")
            if (countries.any { it.eu }) add("GHIC card")
        }
        return base + extra
    }

    fun readinessItems(stops: List<Stop>, packs: Map<String, CountryPack>): List<Pair<String, String>> {
        val cs = tripCountries(stops).mapNotNull { packs[it] }
        val n = cs.size
        val countries = if (n == 1) "1 country" else "$n countries"
        return buildList {
            add("passport" to "Passport valid long enough (see Passport)")
            add("insurance" to "Travel insurance covers all $countries")
            cs.filter { it.eu }.takeIf { it.isNotEmpty() }?.let { add("ghic" to "GHIC card packed for ${it.joinToString { c -> c.name }}") }
            cs.filter { !it.eu }.forEach { add("entry-${it.code}" to "Entry rules done for ${it.name}") }
            if (stops.size > 1) add("legs" to "Transport between all stops booked")
            add("bank" to "Bank told about travel to ${cs.joinToString { it.name }}")
            add("roaming" to "Roaming or eSIM sorted for $countries")
            add("ice" to "Emergency contact and home number saved")
            add("adapters" to "Plug adapters packed: Type ${tripPlugs(stops, packs).joinToString("/")}")
            add("embassy" to "British embassy details noted for each country")
        }
    }

    /** Passport checks against the route. Returns (ok, message) pairs. */
    fun passportChecks(expiry: LocalDate?, stops: List<Stop>, packs: Map<String, CountryPack>): List<Pair<Boolean, String>> {
        if (expiry == null) return emptyList()
        val out = mutableListOf<Pair<Boolean, String>>()
        val schengen = stops.filter { packs[it.country]?.schengen == true }
        schengen.mapNotNull { date(it.to) }.maxOrNull()?.let { lastOut ->
            val need = lastOut.plusMonths(3)
            out += (!expiry.isBefore(need)) to "Schengen: valid 3 months after you leave (${need.format(shortDate)})"
        }
        stops.forEach { s ->
            val p = packs[s.country] ?: return@forEach
            val arrive = date(s.from) ?: return@forEach
            if (p.passportMonthsOnArrival > 0) {
                val need = arrive.plusMonths(p.passportMonthsOnArrival.toLong())
                out += (!expiry.isBefore(need)) to "${p.name}: ${p.passportMonthsOnArrival} months' validity on arrival (${need.format(shortDate)})"
            }
        }
        tripEnd(stops)?.let { end ->
            out += (!expiry.isBefore(end)) to "Valid until you return home"
            val six = end.plusMonths(6)
            out += (!expiry.isBefore(six)) to "6 months left after you get home (${six.format(shortDate)}), as many countries require"
        }
        return out
    }

    /** Whole months of passport validity left after the trip ends. */
    fun monthsAfterTrip(expiry: LocalDate?, stops: List<Stop>): Int? {
        val end = tripEnd(stops) ?: return null
        if (expiry == null) return null
        return ChronoUnit.MONTHS.between(end, expiry).toInt()
    }
}
