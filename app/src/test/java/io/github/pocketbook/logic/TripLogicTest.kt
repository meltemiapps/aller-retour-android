package io.github.pocketbook.logic

import io.github.pocketbook.data.CountryPack
import io.github.pocketbook.data.Driving
import io.github.pocketbook.data.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripLogicTest {

    private fun pack(code: String, schengen: Boolean) = CountryPack(
        code = code, name = code, defaultCity = code, flag = "", timeZone = "Europe/Athens", currency = "EUR",
        currencySymbol = "€", indicativeRatePerGbp = 1.15, plugs = listOf("C"), voltage = 230, frequency = "50",
        exitCode = "00", callingCode = "30", lat = 0.0, lon = 0.0, schengen = schengen,
        emergency = emptyList(), driving = Driving("Right", "", ""),
    )

    private val packs = mapOf("GR" to pack("GR", true), "US" to pack("US", false))

    @Test fun extendingAStopShiftsLaterStops() {
        val stops = listOf(
            Stop("a", "GR", from = "2027-05-02", to = "2027-05-06"),
            Stop("b", "GR", from = "2027-05-06", to = "2027-05-09"),
        )
        val out = TripLogic.updateStopDates(stops, 0, "2027-05-02", "2027-05-08", shiftLater = true)
        assertEquals("2027-05-08", out[1].from)
        assertEquals("2027-05-11", out[1].to)
    }

    @Test fun extendingWithoutShiftCreatesOverlapWarning() {
        val stops = listOf(
            Stop("a", "GR", from = "2027-05-02", to = "2027-05-06"),
            Stop("b", "GR", from = "2027-05-06", to = "2027-05-09"),
        )
        val out = TripLogic.updateStopDates(stops, 0, "2027-05-02", "2027-05-08", shiftLater = false)
        assertEquals("Overlaps the next stop by 2 day(s)", TripLogic.routeWarnings(out)[0])
    }

    @Test fun schengenCountsArrivalAndDepartureDays() {
        val stops = listOf(Stop("a", "GR", from = "2027-05-02", to = "2027-05-06"), Stop("b", "US", from = "2027-05-06", to = "2027-05-20"))
        assertEquals(5, TripLogic.schengenMaxDays(stops, packs, 0))
        assertEquals(15, TripLogic.schengenMaxDays(stops, packs, 10))
    }

    @Test fun ukNumberFormattedForDiallingAbroad() {
        val d = TripLogic.ukDial("07700 900123", "011")!!
        assertEquals("+44 7700 900 123", d.international)
        assertEquals("011 44 7700900123", d.fromHere)
        assertEquals("tel:+447700900123", d.telUri)
        assertNull(TripLogic.ukDial("abc", "00"))
    }

    @Test fun distanceAndBearing() {
        val m = TripLogic.distanceMetres(35.5138, 24.0180, 35.5238, 24.0180)
        assertEquals(1112.0, m, 5.0)
        assertEquals("N", TripLogic.compassPoint(TripLogic.bearingDegrees(35.5138, 24.0180, 35.5238, 24.0180)))
    }
}
