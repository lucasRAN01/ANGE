package com.ange.mobile

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class CompanionPolicyTest {
    @Test fun httpsAllowed() { assertEquals("https://mon-pc.local:8765", CompanionPolicy.validateServer("https://mon-pc.local:8765/", false)) }
    @Test fun emulatorHttpDebugOnly() {
        assertEquals("http://10.0.2.2:8765", CompanionPolicy.validateServer("http://10.0.2.2:8765", true))
        try { CompanionPolicy.validateServer("http://10.0.2.2:8765", false); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun rejectsUnsafeAddresses() {
        listOf("http://192.168.1.2:8765", "https://user:password@pc.local", "https://pc.local/path", "https://pc.local?key=x", "file:///etc/passwd").forEach {
            try { CompanionPolicy.validateServer(it, true); fail(it) } catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun animationCanBeStopped() { for(tick in 0..500) assertEquals(0, CompanionPolicy.frame("SPEAKING",tick,0,false,true)) }
    @Test fun speakingUsesAllMouthPoses() {
        val frames=(0..30).map { CompanionPolicy.frame("SPEAKING",it,0,true,false) }.toSet()
        assertEquals(setOf(0,2,3,4),frames)
    }
    @Test fun idleBlinksAndStatesRemainInAtlas() {
        assertEquals(1,CompanionPolicy.frame("IDLE",88,0,true,false))
        assertEquals(5,CompanionPolicy.frame("LISTENING",0,0,true,false))
        assertEquals(7,CompanionPolicy.frame("THINKING",0,0,true,false))
        assertEquals(6,CompanionPolicy.frame("IDLE",0,0,true,true))
        for(t in 0..500) assertTrue(CompanionPolicy.frame("SPEAKING",t,100,true,false) in 0..7)
    }
    @Test fun offlineAnswersAreLimitedAndDeterministic() {
        assertTrue(CompanionPolicy.offline("Quelle heure est-il ?","fr",Date(0)).startsWith("Il est "))
        assertTrue(CompanionPolicy.offline("What time is it?","en",Date(0)).startsWith("It is "))
        assertTrue(CompanionPolicy.offline("Salama","mg",Date(0)).startsWith("Salama!"))
        assertTrue(CompanionPolicy.offline("Ouvre mon PC","fr",Date(0)).contains("sans IA générative"))
    }
}
