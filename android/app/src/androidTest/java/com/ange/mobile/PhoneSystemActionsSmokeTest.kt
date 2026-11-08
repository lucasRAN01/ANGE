package com.ange.mobile

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class PhoneSystemActionsSmokeTest {
    @Test fun lockPermissionIsLaunchedFromAnActivityWithoutNewTask() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        org.junit.Assume.assumeFalse(AngeLockAdmin.active(context))
        val monitor=instrumentation.addMonitor(IntentFilter(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN),android.app.Instrumentation.ActivityResult(0,null),true)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.startActivity(AndroidDeviceActions(context).intentFor(DeviceAction.LockPhone))
                }
                for(i in 0..30) { if(monitor.hits>0) break; Thread.sleep(100) }
                assertEquals("Android admin consent must be reached from the host activity",1,monitor.hits)
                assertEquals(0,AngeLockAdmin.request(context).flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }
    @Test fun connectivityCommandsOpenHonestUserControlledPanels() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val actions=AndroidDeviceActions(context)
        assertEquals(if(Build.VERSION.SDK_INT>=29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS,actions.intentFor(DeviceAction.RadioPanel("Wi-Fi",true))!!.action)
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS,actions.intentFor(DeviceAction.RadioPanel("Bluetooth",false))!!.action)
        assertEquals(if(Build.VERSION.SDK_INT>=29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_DATA_ROAMING_SETTINGS,actions.intentFor(DeviceAction.RadioPanel("Données mobiles",true))!!.action)
    }
    @Suppress("DEPRECATION")
    @Test fun lockingIsLimitedToForceLockAndCaptureComponentsArePrivate() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN,AngeLockAdmin.request(context).action)
        val tags=mutableSetOf<String>()
        context.resources.getXml(R.xml.ange_lock_admin).use { parser -> while(parser.eventType!=XmlPullParser.END_DOCUMENT) { if(parser.eventType==XmlPullParser.START_TAG) tags+=parser.name; parser.next() } }
        assertEquals(setOf("device-admin","uses-policies","force-lock"),tags)
        val info=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_SERVICES or PackageManager.GET_ACTIVITIES)
        val capture=info.services.orEmpty().single { it.name.endsWith("ScreenshotService") }
        assertFalse(capture.exported); assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,capture.foregroundServiceType)
        assertFalse(info.activities.orEmpty().single { it.name.endsWith("ScreenshotActivity") }.exported)
        assertEquals(ScreenshotActivity::class.java.name,AndroidDeviceActions(context).intentFor(DeviceAction.Screenshot)!!.component!!.className)
    }
}
