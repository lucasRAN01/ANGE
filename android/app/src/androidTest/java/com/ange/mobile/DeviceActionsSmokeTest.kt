package com.ange.mobile

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceActionsSmokeTest {
    @Test fun intentsPrepareCommunicationWithoutSending() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val actions=AndroidDeviceActions(context)
        assertEquals(Intent.ACTION_DIAL,actions.intentFor(DeviceAction.Dial("5550100"))!!.action)
        val sms=actions.intentFor(DeviceAction.Sms("5550100","Test : à bientôt"))!!
        assertEquals(Intent.ACTION_SENDTO,sms.action); assertEquals("smsto",sms.data!!.scheme)
        assertEquals("Test : à bientôt",sms.getStringExtra("sms_body"))
        assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission("android.permission.SEND_SMS"))
        assertEquals(PackageManager.PERMISSION_DENIED,context.checkSelfPermission("android.permission.CALL_PHONE"))
        val alarm=actions.intentFor(DeviceAction.Alarm(7,30))!!
        assertEquals(7,alarm.getIntExtra(AlarmClock.EXTRA_HOUR,-1)); assertFalse(alarm.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI,true))
    }
    @Test fun launcherEnumerationAndVolumeExecuteOnDevice() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val actions=AndroidDeviceActions(context)
        assertTrue(actions.apps().isNotEmpty())
        val audio=context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        val before=audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        try {
            actions.execute(DeviceAction.Volume(40))
            val expected=(audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)*.4f).toInt()
            assertEquals(expected,audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC))
        } finally { audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,before,0) }
    }
    @Test fun confirmationCancelAndBackgroundStopHandsFree() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync { controller=CompanionController(instrumentation.targetContext) }
        try {
            val ready=java.util.concurrent.atomic.AtomicBoolean(false)
            for(attempt in 0..50) {
                instrumentation.runOnMainSync { ready.set(controller.speechReady) }
                if(ready.get()) break
                Thread.sleep(200)
            }
            instrumentation.runOnMainSync {
                controller.autoSpeak=false
                controller.send("Prépare un SMS au 5550100 : Test")
                assertTrue(controller.pendingAction is DeviceAction.Sms)
                controller.cancelAction(); assertNull(controller.pendingAction)
                controller.send("Alarme à 7h30"); assertTrue(controller.pendingAction is DeviceAction.Alarm)
                assertTrue("A working Android TTS engine is needed for hands-free mode",controller.enableHandsFree())
                assertTrue(controller.handsFree)
                controller.onBackground(); assertNull(controller.pendingAction); assertFalse(controller.handsFree); assertFalse(controller.active)
            }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
}
