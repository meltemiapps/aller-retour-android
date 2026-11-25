package io.github.allerretour.logic

import io.github.allerretour.data.AppState
import io.github.allerretour.data.CountryPack
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Every optional reminder. Same list and defaults as the iPhone, iPad and Mac app. */
enum class ReminderKind(val title: String, val defaultOn: Boolean) {
    PASSPORT9("Passport expiry: 9 months before", true),
    PASSPORT6("Passport expiry: 6 months before", true),
    CHECKIN("Online check-in opens", true),
    LEAVE("Leave for the airport", true),
    PACK("Start packing", true),
    MOVEON("Moving on to the next stop", true),
    SCHENGEN("Schengen days running low", true),
    INSURANCE("Travel insurance ends", false),
    GHIC("GHIC card expires", false),
    MEDS("Take your medication", false),
    SAFE("Let home know you're safe", true),
}

data class PlannedReminder(val kind: ReminderKind, val at: LocalDateTime?, val daily: LocalTime?, val body: String)

/** Pure planning logic: works out when each reminder would fire. Scheduling lives in notify/. */
object ReminderPlan {
    private val whenFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK)
    private val longFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    fun isOn(s: AppState, k: ReminderKind): Boolean = s.reminders[k.name] ?: k.defaultOn

    fun flight(s: AppState): LocalDateTime? = runCatching { LocalDateTime.parse(s.flightDeparture) }.getOrNull()

    fun time(hhmm: String, fallback: LocalTime): LocalTime = runCatching { LocalTime.parse(hhmm) }.getOrDefault(fallback)

    fun plan(s: AppState, packs: Map<String, CountryPack>, today: LocalDate = LocalDate.now()): List<PlannedReminder> {
        val out = mutableListOf<PlannedReminder>()
        val exp = TripLogic.date(s.passportExpiry)
        if (exp != null) {
            out += PlannedReminder(ReminderKind.PASSPORT9, exp.minusMonths(9).atTime(10, 0), null, "Your passport expires on ${exp.format(longFmt)}. Renew now so it never limits where you can go.")
            out += PlannedReminder(ReminderKind.PASSPORT6, exp.minusMonths(6).atTime(10, 0), null, "Six months left on your passport. Many countries refuse entry below this. Renew it now.")
        } else {
            out += PlannedReminder(ReminderKind.PASSPORT9, null, null, "Add your passport expiry date in Kit")
            out += PlannedReminder(ReminderKind.PASSPORT6, null, null, "Add your passport expiry date in Kit")
        }
        val f = flight(s)
        out += PlannedReminder(ReminderKind.CHECKIN, f?.minusHours(24), null, "Online check-in usually opens now for your flight.")
        out += PlannedReminder(ReminderKind.LEAVE, f?.minusHours(3), null, "Time to leave for the airport. Passport, phone, charger.")
        val start = TripLogic.tripStart(s.stops)
        out += PlannedReminder(ReminderKind.PACK, start?.minusDays(3)?.atTime(19, 0), null, "Three days to go. Open Kit and start packing.")
        val i = s.stops.indexOfFirst { (TripLogic.date(it.to) ?: LocalDate.MIN) >= today }
        val leave = s.stops.getOrNull(i)?.let { TripLogic.date(it.to) }
        val next = s.stops.getOrNull(i + 1)
        out += if (i >= 0 && next != null && leave != null) {
            val name = next.city.ifBlank { packs[next.country]?.name ?: "your next stop" }
            PlannedReminder(ReminderKind.MOVEON, leave.atTime(9, 0), null, "Today you move on to $name.")
        } else PlannedReminder(ReminderKind.MOVEON, null, null, "Add more than one stop in Plan")
        val used = TripLogic.schengenMaxDays(s.stops, packs, s.schengenDaysAlreadyUsed)
        out += PlannedReminder(ReminderKind.SCHENGEN, if (used >= 80) start?.atTime(9, 0) else null, null, "You will reach $used of 90 Schengen days on this trip. Check your dates.")
        out += PlannedReminder(ReminderKind.INSURANCE, TripLogic.date(s.insuranceEnds)?.minusDays(14)?.atTime(10, 0), null, "Your travel insurance ends in two weeks.")
        out += PlannedReminder(ReminderKind.GHIC, TripLogic.date(s.ghicExpiry)?.minusMonths(1)?.atTime(10, 0), null, "Your GHIC card expires next month. Renewal is free on NHS.uk.")
        out += PlannedReminder(ReminderKind.MEDS, null, time(s.medsTime, LocalTime.of(8, 0)), "Time to take your medication.")
        out += PlannedReminder(ReminderKind.SAFE, null, time(s.safeTime, LocalTime.of(21, 0)), "Let someone at home know you're safe today.")
        return out
    }

    fun whenLine(p: PlannedReminder, now: LocalDateTime = LocalDateTime.now()): String {
        p.daily?.let { return "Daily at %02d:%02d".format(it.hour, it.minute) }
        val at = p.at ?: return when (p.kind) {
            ReminderKind.CHECKIN, ReminderKind.LEAVE -> "Add your flight time below"
            ReminderKind.SCHENGEN -> "Only if a trip takes you to 80 of 90 days"
            ReminderKind.INSURANCE -> "Add the policy end date below"
            ReminderKind.GHIC -> "Add the card expiry date below"
            else -> p.body
        }
        return at.format(whenFmt) + if (at.isBefore(now)) " · passed" else ""
    }

    /** The next few switched-on, dated reminders, for the Today screen. */
    fun upcoming(s: AppState, packs: Map<String, CountryPack>, now: LocalDateTime = LocalDateTime.now(), limit: Int = 3): List<PlannedReminder> =
        plan(s, packs, now.toLocalDate()).filter { isOn(s, it.kind) && it.at != null && it.at.isAfter(now) }.sortedBy { it.at }.take(limit)
}
