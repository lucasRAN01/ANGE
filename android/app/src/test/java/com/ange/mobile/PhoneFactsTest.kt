package com.ange.mobile

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Date
import java.util.TimeZone

class PhoneFactsTest {
    @Test fun recognizesSpokenAndTypedTimeRequests() {
        listOf("quelle heure est-il", "Quelle heure est-il ?", "Il est quelle heure ?",
            "Donne-moi l’heure, s’il te plaît.", "Peux-tu me dire quelle heure il est ?",
            "Hey ANGE, quelle heure est-il maintenant ?", "Bonjour ANGE quelle heure est-il ?",
            "What time is it now?", "Please tell me the time").forEach {
            assertEquals(it, PhoneFact.TIME, PhoneFacts.parse(it))
        }
    }

    @Test fun recognizesCurrentDateAndCombinedRequests() {
        listOf("Quelle est la date d’aujourd’hui ?", "Quel jour sommes-nous ?", "On est le combien ?",
            "Dis-moi la date", "What is today's date?").forEach {
            assertEquals(it, PhoneFact.DATE, PhoneFacts.parse(it))
        }
        assertEquals(PhoneFact.DATE_TIME, PhoneFacts.parse("Donne-moi l’heure et la date"))
        assertEquals(PhoneFact.DATE_TIME, PhoneFacts.parse("Date et heure"))
    }

    @Test fun matchesBatteryLevelRequests() {
        listOf("Combien de batterie me reste-t-il ?", "Quel est le niveau de batterie ?", "Battery level").forEach {
            assertEquals(it, PhoneFact.BATTERY, PhoneFacts.parse(it))
        }
    }

    @Test fun doesNotConfuseOtherQuestionsOrActionsWithCurrentDeviceFacts() {
        listOf("Le bonheur", "Comment économiser ma batterie ?", "Quelle heure est-il à Paris ?",
            "Quelle heure sera-t-il dans deux heures ?", "Quel jour était le 1 janvier 2000 ?",
            "Alarme à 7 heures", "Prépare un SMS au 5550100 : quelle heure est-il",
            "Cherche la date de sortie du film", "Tomorrow's date", "How to change the time?").forEach {
            assertNull(it, PhoneFacts.parse(it))
        }
    }

    @Test fun formatsTheDevicesTimeZoneAcrossMidnight() {
        val instant = Date.from(Instant.parse("2026-11-08T21:05:00Z"))
        val local = PhoneSnapshot(instant, TimeZone.getTimeZone("Indian/Antananarivo"))
        assertEquals("Il est 00:05.", PhoneFacts.reply(PhoneFact.TIME, local, "fr"))
        assertEquals("Nous sommes le lundi 9 novembre 2026.", PhoneFacts.reply(PhoneFact.DATE, local, "fr"))
        val utc = local.copy(timeZone = TimeZone.getTimeZone("UTC"))
        assertEquals("Il est 21:05.", PhoneFacts.reply(PhoneFact.TIME, utc, "fr"))
        assertEquals("Nous sommes le dimanche 8 novembre 2026.", PhoneFacts.reply(PhoneFact.DATE, utc, "fr"))
    }

    @Test fun repeatedQuestionsUseFreshSnapshotValues() {
        val first = PhoneSnapshot(Date.from(Instant.parse("2026-11-08T10:00:00Z")), TimeZone.getTimeZone("UTC"))
        val later = first.copy(now = Date.from(Instant.parse("2026-11-08T10:04:00Z")))
        assertEquals("Il est 10:00.", PhoneFacts.reply(PhoneFact.TIME, first, "fr"))
        assertEquals("Il est 10:04.", PhoneFacts.reply(PhoneFact.TIME, later, "fr"))
    }

    @Test fun unknownBatteryNeverBecomesAnInventedPercentage() {
        val snapshot = PhoneSnapshot(Date(0), TimeZone.getTimeZone("UTC"))
        for (level in listOf(null, -1, 101, Int.MIN_VALUE)) {
            assertTrue(PhoneFacts.reply(PhoneFact.BATTERY, snapshot.copy(batteryPercent=level), "fr").contains("ne permet pas de lire"))
        }
        assertEquals("La batterie du téléphone est à 73 %. Elle est en charge.",
            PhoneFacts.reply(PhoneFact.BATTERY, snapshot.copy(batteryPercent=73, charging=true), "fr"))
        assertTrue(PhoneFacts.reply(PhoneFact.BATTERY, snapshot.copy(batteryPercent=0, charging=false), "fr").startsWith("La batterie du téléphone est à 0 %."))
    }

    @Test fun replyLanguageDoesNotChangeTheClockSource() {
        val snapshot = PhoneSnapshot(Date.from(Instant.parse("2026-11-08T10:00:00Z")), TimeZone.getTimeZone("UTC"))
        assertTrue(PhoneFacts.reply(PhoneFact.DATE_TIME, snapshot, "en").startsWith("Today is Sunday, November 8, 2026."))
        assertEquals("Amin’ny 10:00 izao.", PhoneFacts.reply(PhoneFact.TIME, snapshot, "mg"))
    }
}
