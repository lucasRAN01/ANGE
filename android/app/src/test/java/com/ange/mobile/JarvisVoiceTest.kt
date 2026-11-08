package com.ange.mobile
import org.junit.Assert.*
import org.junit.Test

class JarvisVoiceTest {
    @Test fun upstreamNormalizationAndVoiceCleanup() {
        assertEquals("frederic",JarvisVoice.withoutAccents("Ｆrédéric"))
        assertEquals("Voici la réponse.",JarvisVoice.clean("Un instant... je vérifie. voici la réponse."))
        assertEquals("Bonjour",JarvisVoice.clean("Bonjour"))
    }
    @Test fun knownSensitivePatternsAreNotReadOut() {
        val input="test@example.com sk-"+"a".repeat(20)+" 0341234567"
        val result=JarvisVoice.spoken(input,true)
        assertFalse(result.contains("test@example.com")); assertFalse(result.contains("sk-")); assertFalse(result.contains("0341234567"))
        assertTrue(result.contains("[adresse mail]")); assertTrue(result.contains("[cle]"))
    }
    @Test fun privacySwitchPreservesExplicitUnfilteredReadout() {
        assertEquals("Contact test@example.com",JarvisVoice.spoken("contact test@example.com",false))
    }
}
