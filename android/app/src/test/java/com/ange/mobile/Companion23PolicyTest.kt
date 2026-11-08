package com.ange.mobile
import org.junit.Assert.*
import org.junit.Test

class Companion23PolicyTest {
    @Test fun contactMatchingIsLocalExactAndKeepsAmbiguousNumbers() {
        val contacts=listOf(ContactNumber("Babe ❤️","034 000 0000"),ContactNumber("Babe", "+261340000001"),ContactNumber("Barbara","0340000002"))
        val found=ContactDirectory.match("babe",contacts)
        assertEquals(2,found.size); assertTrue(found.any { it.number=="0340000000" })
        assertTrue(ContactDirectory.match("Ba",contacts).isEmpty())
        assertEquals(1,ContactDirectory.match("Frédéric",listOf(ContactNumber("Frederic","5550100"))).size)
    }
    @Test fun contactNumbersCannotInjectDialerCodesOrUris() {
        assertTrue(ContactDirectory.match("Babe",listOf(ContactNumber("Babe","tel:5550100"),ContactNumber("Babe","*21*5550100#"))).isEmpty())
        assertEquals(1,ContactDirectory.match("Babe",listOf(ContactNumber("Babe","5550100"),ContactNumber("Babe","5550100"))).size)
    }
    @Test fun navigationRequiresExplicitCommands() {
        assertEquals(DeviceAction.Navigate("retour"),(DeviceActionPolicy.parse("Retour",emptyList()) as ActionParse.Found).action)
        for(text in listOf("Va à l’accueil", "Va à l'accueil", "Va à l accueil")) assertEquals(DeviceAction.Navigate("accueil"),(DeviceActionPolicy.parse(text,emptyList()) as ActionParse.Found).action)
        assertEquals(ActionParse.None,DeviceActionPolicy.parse("Explique comment faire retour",emptyList()))
        assertEquals(ActionParse.None,DeviceActionPolicy.parse("Appuie sur Envoyer",emptyList()))
    }
    @Test fun politeRequestsStillBecomeNativeActions() {
        assertEquals(DeviceAction.CallContact("babe"),(DeviceActionPolicy.parse("Peux-tu appeler Babe ?",emptyList()) as ActionParse.Found).action)
        assertEquals(DeviceAction.Camera,(DeviceActionPolicy.parse("Tu peux ouvrir l’appareil photo ?",emptyList()) as ActionParse.Found).action)
        assertEquals(ActionParse.None,DeviceActionPolicy.parse("Peux-tu expliquer comment appeler Babe ?",emptyList()))
    }
    @Test fun photoAnimationHasAllTwelvePosesAndCanStop() {
        val poses=mutableSetOf<Int>()
        for(t in 0L..6000L step 60) for(mood in CompanionMood.entries) {
            poses+=PhotoMotion.frame(mood,t,0,0,false,true)
            poses+=PhotoMotion.frame(mood,t,0,-1,false,true)
            poses+=PhotoMotion.frame(mood,t,0,1,false,true)
            poses+=PhotoMotion.frame(mood,t,0,0,true,true)
            assertEquals(0,PhotoMotion.frame(mood,t,0,1,true,false))
        }
        assertEquals((0..11).toSet(),poses)
    }
}
