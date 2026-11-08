package com.ange.mobile
import org.junit.Assert.*
import org.junit.Test

class PhoneActions232Test {
    private fun action(text: String)=(DeviceActionPolicy.parse(text,emptyList()) as ActionParse.Found).action
    @Test fun shortAppNamesAndSpokenSpacingResolveInstalledApps() {
        val notes=LaunchableApp("com.samsung.notes","Samsung Notes")
        val video=LaunchableApp("com.example.youtube","YouTube")
        val apps=listOf(notes,video)
        assertEquals(AppMatch.One(notes),AppResolver.resolve("Notes",apps))
        assertEquals(AppMatch.One(notes),AppResolver.resolve("Note",apps))
        assertEquals(AppMatch.One(notes),AppResolver.resolve("Notes s’il te plaît",apps))
        assertEquals(AppMatch.One(notes),AppResolver.resolve("les Notes",apps))
        assertEquals(AppMatch.One(video),AppResolver.resolve("You Tube",apps))
        assertEquals(DeviceAction.OpenApp(notes),(DeviceActionPolicy.parse("Ouvre Notes",apps) as ActionParse.Found).action)
    }
    @Test fun ambiguityAndApproximationNeverPickAnAppAtRandom() {
        val apps=listOf(LaunchableApp("samsung","Samsung Notes"),LaunchableApp("other","Notes privées"))
        assertTrue(AppResolver.resolve("Notes",apps) is AppMatch.Choose)
        assertEquals(AppMatch.One(apps[0]),AppResolver.resolve("Samsung Notes",apps))
        assertTrue(AppResolver.resolve("Nots",apps) is AppMatch.Choose)
        assertEquals(AppMatch.None,AppResolver.resolve("inconnue",apps))
        assertEquals(AppMatch.None,AppResolver.resolve("No",apps))
    }
    @Test fun torchAndLockAreExplicitNativeCommands() {
        assertEquals(DeviceAction.Torch(true),action("Allume la lampe"))
        assertEquals(DeviceAction.Torch(false),action("Éteins la lampe"))
        assertEquals(DeviceAction.LockPhone,action("Verrouille le téléphone"))
        assertEquals(DeviceAction.LockPhone,action("Verrouille l’écran"))
        assertEquals(ActionParse.None,DeviceActionPolicy.parse("Ne verrouille pas le téléphone",emptyList()))
    }
    @Test fun captureAlwaysRoutesToTheAndroidConsentFlow() {
        for(text in listOf("Capture d’écran","Fais une capture d'écran","Prends une capture d’écran")) assertEquals(DeviceAction.Screenshot,action(text))
        assertEquals(ActionParse.None,DeviceActionPolicy.parse("Explique une capture d’écran",emptyList()))
    }
    @Test fun connectivityIsRepresentedAsAUserControlledPanel() {
        assertEquals(DeviceAction.RadioPanel("Wi-Fi",true),action("Active le Wi-Fi"))
        assertEquals(DeviceAction.RadioPanel("Bluetooth",false),action("Désactive le Bluetooth"))
        assertEquals(DeviceAction.RadioPanel("Données mobiles",true),action("Active les données mobiles"))
    }
}
