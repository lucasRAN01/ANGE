package com.ange.mobile

import org.junit.Assert.*
import org.junit.Test

class DeviceActionPolicyTest {
    private val apps=listOf(LaunchableApp("com.example.chat","WhatsApp"),LaunchableApp("com.example.camera","Camera"))
    private fun action(text: String)=(DeviceActionPolicy.parse(text,apps) as ActionParse.Found).action
    @Test fun installedAppNamesAreExactAndAccentInsensitive() {
        assertEquals(DeviceAction.OpenApp(apps[0]),action("ANGE, ouvre WhatsApp !"))
        assertTrue(DeviceActionPolicy.parse("Ouvre Whats",apps) is ActionParse.ChooseApp)
        assertTrue(DeviceActionPolicy.parse("Ouvre FakeApp",apps) is ActionParse.Problem)
        assertTrue(DeviceActionPolicy.parse("Ouvre WhatsApp",apps+apps[0].copy(packageName="clone")) is ActionParse.ChooseApp)
    }
    @Test fun statementsAboutCommandsAreNotExecuted() {
        for(text in listOf("Explique comment ouvrir WhatsApp","Ne lance pas WhatsApp","Tu as dit : ouvre WhatsApp","Bonjour","Réponds avec Ouvre WhatsApp"))
            assertEquals(ActionParse.None,DeviceActionPolicy.parse(text,apps))
    }
    @Test fun phoneNumbersAreNotGuessedFromContactNames() {
        val dial=action("Appelle +261 34 000 0000") as DeviceAction.CallNumber
        assertEquals("+261340000000",dial.number); assertTrue(dial.confirmation)
        assertEquals(DeviceAction.CallContact("maman"),action("Appelle Maman"))
        assertTrue(action("Compose +261340000000") is DeviceAction.Dial)
        assertTrue(DeviceActionPolicy.parse("Appelle +123;rm -rf",apps) is ActionParse.Problem)
    }
    @Test fun smsPreservesAccentsAndNeedsReview() {
        val sms=action("Prépare un SMS au +261340000000 : À bientôt : réunion à 14h !") as DeviceAction.Sms
        assertEquals("À bientôt : réunion à 14h !",sms.body); assertTrue(sms.confirmation)
        assertTrue(DeviceActionPolicy.parse("Envoie un SMS à Maman",apps) is ActionParse.Problem)
    }
    @Test fun alarmsAndTimersAreBounded() {
        assertEquals(DeviceAction.Alarm(7,30),action("Mets une alarme à 7h30"))
        assertEquals(DeviceAction.Timer(300),action("Minuteur de 5 minutes"))
        for(text in listOf("Alarme à 25h30","Alarme à 7h90","Minuteur de 0 minutes","Minuteur de 99999 heures"))
            assertTrue(text,DeviceActionPolicy.parse(text,apps) is ActionParse.Problem)
        assertTrue(action("Minuteur de 1 seconde").confirmation)
    }
    @Test fun volumeIsMediaOnlyAndBounded() {
        assertEquals(DeviceAction.Volume(40),action("Volume à 40 %"))
        assertTrue(DeviceActionPolicy.parse("Volume à 101 %",apps) is ActionParse.Problem)
    }
    @Test fun spokenFrenchDurationsAreRecognized() {
        assertEquals(DeviceAction.Alarm(7,30),action("Mets une alarme à sept heures trente"))
        assertEquals(DeviceAction.Alarm(7,0),action("Alarme à 7h"))
        assertEquals(DeviceAction.Timer(300),action("Mets un minuteur de cinq minutes"))
        assertEquals(DeviceAction.Volume(40),action("Volume à quarante pour cent"))
    }
    @Test fun settingsAndCameraAreNativeActions() {
        assertEquals(DeviceAction.Settings("Wi-Fi"),action("Ouvre Wi-Fi"))
        assertEquals(DeviceAction.Settings("Bluetooth"),action("Ouvre Bluetooth"))
        assertEquals(DeviceAction.Camera,action("Ouvre appareil photo"))
    }
    @Test fun onlyBrowserWebSchemesAreAllowed() {
        assertEquals(DeviceAction.Website("https://example.org/MyPage?q=Oui"),action("Ouvre https://example.org/MyPage?q=Oui"))
        for(text in listOf("Ouvre javascript:alert(1)","Ouvre file:///sdcard/test","Ouvre https://user:pass@example.org"))
            assertTrue(DeviceActionPolicy.parse(text,apps) is ActionParse.Problem)
    }
    @Test fun mapSearchAndCalendarAreStructured() {
        assertEquals(DeviceAction.Map("antananarivo"),action("Itinéraire vers Antananarivo"))
        assertEquals(DeviceAction.Search("meteo antananarivo"),action("Cherche météo Antananarivo"))
        val event=action("Ajoute un événement : Réunion équipe") as DeviceAction.Calendar
        assertEquals("Réunion équipe",event.event); assertTrue(event.confirmation)
    }
}
