package com.ange.mobile

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandardPermissionsSmokeTest {
    @Suppress("DEPRECATION")
    @Test fun installedStandardPackageHasNoAccessibilitySmsReadingOrNotificationListener() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val info=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS)
        val forbidden=setOf("android.permission.BIND_ACCESSIBILITY_SERVICE","android.permission.BIND_NOTIFICATION_LISTENER_SERVICE","android.permission.READ_SMS","android.permission.RECEIVE_SMS")
        assertTrue(info.requestedPermissions.orEmpty().none { it in forbidden })
        assertTrue(info.services.orEmpty().none { it.permission in forbidden || it.name.endsWith("AngeNavigationService") })
        val floating=info.services.orEmpty().single { it.name.endsWith("FloatingCompanionService") }
        assertFalse("The floating service must remain private to ANGE",floating.exported)
        assertTrue("Contact calling remains an explicit runtime permission",info.requestedPermissions.orEmpty().contains("android.permission.CALL_PHONE"))
    }
    @Test fun homeUsesAPublicAndroidIntentAndOtherNavigationDoesNotExecute() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val actions=AndroidDeviceActions(context)
        val home=actions.intentFor(DeviceAction.Navigate("accueil"))!!
        assertEquals(Intent.ACTION_MAIN,home.action)
        assertTrue(home.hasCategory(Intent.CATEGORY_HOME))
        for(direction in listOf("retour","récentes","descendre","monter")) {
            assertNull(actions.intentFor(DeviceAction.Navigate(direction)))
            assertTrue(runCatching { actions.execute(DeviceAction.Navigate(direction)) }.exceptionOrNull() is IllegalArgumentException)
        }
    }
}
