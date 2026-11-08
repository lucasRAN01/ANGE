package com.ange.mobile

import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeechInputSmokeTest {
    @Test fun recognitionIntentUsesARegionalLanguageAndTheCorrectOfflinePreference() {
        val offline = SpeechInputIntents.recognize("fr", true)
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, offline.action)
        assertEquals("fr-FR", offline.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(offline.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false))
        val service = SpeechInputIntents.recognize("fr", false)
        assertFalse(service.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        assertTrue(SpeechInputPolicy.retryWithService(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, true, true))
        assertTrue(SpeechInputPolicy.retryWithService(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, true, true))
        assertEquals(Settings.ACTION_VOICE_INPUT_SETTINGS, SpeechInputIntents.settings().action)
    }

    @Test fun consentIsSessionOnlyAndRevocationStopsHandsFreeWithoutCreatingAnAnswer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val controller = CompanionController(instrumentation.targetContext)
            try {
                assertFalse(controller.allowNetworkRecognition)
                controller.setNetworkRecognitionAllowed(true)
                assertTrue(controller.allowNetworkRecognition)
                controller.setNetworkRecognitionAllowed(false)
                assertFalse(controller.allowNetworkRecognition)
                assertFalse(controller.handsFree)
                assertTrue(controller.lines.isEmpty())
                assertEquals("", controller.lastReply)
                controller.onBackground()
                assertFalse(controller.active)
            } finally { controller.close() }
        }
    }
}
