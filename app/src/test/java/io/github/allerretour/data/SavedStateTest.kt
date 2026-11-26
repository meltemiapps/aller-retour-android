package io.github.allerretour.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Data saved by earlier versions must still load, with the new settings at their defaults. */
class SavedStateTest {
    @Test fun loadsStateSavedBeforeGlassAndBackdrop() {
        val old = """{"schemaVersion":1,"stops":[{"id":"a","country":"GR","city":"Chania","from":"2027-05-02","to":"2027-05-06"}],
            "activeIndex":0,"homeNumber":"07700 900123","onboarded":true,"useWallpaperColours":false}"""
        val s = AppJson.decodeFromString(AppState.serializer(), old)
        assertEquals("Chania", s.stops.single().city)
        assertEquals("07700 900123", s.homeNumber)
        assertEquals("Clear", s.glass)
        assertEquals("Sky", s.backdrop)
    }

    @Test fun newSettingsRoundTrip() {
        val s = AppState(glass = "Tinted", backdrop = "Harbour")
        val back = AppJson.decodeFromString(AppState.serializer(), AppJson.encodeToString(AppState.serializer(), s))
        assertEquals(s, back)
    }
}
