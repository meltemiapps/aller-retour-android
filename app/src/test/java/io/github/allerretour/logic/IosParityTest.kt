package io.github.allerretour.logic

import io.github.allerretour.data.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Logic added in 1.1 to match the iPhone app. */
class IosParityTest {
    @Test fun hoursLabelMatchesIos() {
        assertEquals("Same time as UK", TripLogic.hoursLabel(0.0))
        assertEquals("+2h vs UK", TripLogic.hoursLabel(2.0))
        assertEquals("+5.5h vs UK", TripLogic.hoursLabel(5.5))
        assertEquals("−5h vs UK", TripLogic.hoursLabel(-5.0))
    }

    @Test fun monthsAfterTripCountsWholeMonths() {
        val stops = listOf(Stop("a", "GR", from = "2027-05-02", to = "2027-05-10"))
        assertEquals(6, TripLogic.monthsAfterTrip(LocalDate.parse("2027-11-10"), stops))
        assertEquals(5, TripLogic.monthsAfterTrip(LocalDate.parse("2027-11-09"), stops))
        assertNull(TripLogic.monthsAfterTrip(null, stops))
        assertNull(TripLogic.monthsAfterTrip(LocalDate.parse("2027-11-10"), emptyList()))
    }
}
