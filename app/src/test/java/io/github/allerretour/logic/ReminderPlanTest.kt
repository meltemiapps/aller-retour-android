package io.github.allerretour.logic

import io.github.allerretour.data.AppState
import io.github.allerretour.data.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlanTest {
    private val s = AppState(
        stops = listOf(Stop("a", "GR", "Chania", "2027-05-02", "2027-05-06"), Stop("b", "IT", "Rome", "2027-05-06", "2027-05-12")),
        passportExpiry = "2027-10-20",
        flightDeparture = "2027-05-02T07:40",
    )

    @Test fun passportRemindersAtNineAndSixMonths() {
        val p = ReminderPlan.plan(s, emptyMap(), LocalDate.of(2026, 11, 1)).associateBy { it.kind }
        assertEquals(LocalDateTime.of(2027, 1, 20, 10, 0), p[ReminderKind.PASSPORT9]!!.at)
        assertEquals(LocalDateTime.of(2027, 4, 20, 10, 0), p[ReminderKind.PASSPORT6]!!.at)
    }

    @Test fun flightAndPackingTimes() {
        val p = ReminderPlan.plan(s, emptyMap(), LocalDate.of(2026, 11, 1)).associateBy { it.kind }
        assertEquals(LocalDateTime.of(2027, 5, 1, 7, 40), p[ReminderKind.CHECKIN]!!.at)
        assertEquals(LocalDateTime.of(2027, 5, 2, 4, 40), p[ReminderKind.LEAVE]!!.at)
        assertEquals(LocalDateTime.of(2027, 4, 29, 19, 0), p[ReminderKind.PACK]!!.at)
        assertEquals(LocalDateTime.of(2027, 5, 6, 9, 0), p[ReminderKind.MOVEON]!!.at)
        assertNull(p[ReminderKind.INSURANCE]!!.at)
    }

    @Test fun defaultsAndUpcoming() {
        assertTrue(ReminderPlan.isOn(s, ReminderKind.PASSPORT6))
        assertTrue(!ReminderPlan.isOn(s, ReminderKind.MEDS))
        val up = ReminderPlan.upcoming(s, emptyMap(), LocalDateTime.of(2026, 11, 1, 12, 0))
        assertEquals(ReminderKind.PASSPORT9, up.first().kind)
    }
}
