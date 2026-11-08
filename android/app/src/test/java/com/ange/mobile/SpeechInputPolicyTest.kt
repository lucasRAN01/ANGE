package com.ange.mobile

import org.junit.Assert.*
import org.junit.Test

class SpeechInputPolicyTest {
    @Test fun offlineChoiceNeverUsesTheDefaultServiceWithoutConsent() {
        assertEquals(SpeechInputPolicy.Route.LOCAL, SpeechInputPolicy.initialRoute(true, false))
        assertEquals(SpeechInputPolicy.Route.UNAVAILABLE, SpeechInputPolicy.initialRoute(false, false))
        for (error in 1..15) assertFalse(SpeechInputPolicy.retryWithService(error, true, false))
    }

    @Test fun consentAllowsOneFallbackForUnsupportedOrMissingLocalLanguage() {
        assertEquals(SpeechInputPolicy.Route.LOCAL, SpeechInputPolicy.initialRoute(true, true))
        assertEquals(SpeechInputPolicy.Route.ANDROID_SERVICE, SpeechInputPolicy.initialRoute(false, true))
        assertTrue(SpeechInputPolicy.retryWithService(12, true, true))
        assertTrue(SpeechInputPolicy.retryWithService(13, true, true))
        for (error in 1..15) assertFalse(SpeechInputPolicy.retryWithService(error, false, true))
    }

    @Test fun timeoutNoMatchPermissionsAndBusyDoNotTriggerNetworkFallback() {
        for (error in listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15)) {
            assertFalse(SpeechInputPolicy.retryWithService(error, true, true))
        }
    }

    @Test fun languageTagsIncludeTheRequestedRegion() {
        assertEquals("fr-FR", SpeechInputPolicy.languageTag("fr"))
        assertEquals("en-US", SpeechInputPolicy.languageTag("en"))
        assertEquals("mg-MG", SpeechInputPolicy.languageTag("mg"))
        assertEquals("fr-CA", SpeechInputPolicy.languageTag("fr_CA"))
    }

    @Test fun unsupportedAndMissingModelHaveDistinctActionableMessages() {
        val unsupported = SpeechInputPolicy.errorMessage(12, "fr", true)
        val missing = SpeechInputPolicy.errorMessage(13, "fr", true)
        assertTrue(unsupported.contains("ne prend pas en charge fr-FR"))
        assertTrue(unsupported.contains("autorisez le réseau"))
        assertTrue(missing.contains("pas disponible ou téléchargé"))
        assertFalse(SpeechInputPolicy.errorMessage(12, "fr", false).contains("autorisez le réseau"))
        assertFalse(unsupported.contains("réponse IA inventée"))
    }
}
