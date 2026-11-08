package com.ange.mobile

import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPersistenceSmokeTest {
    @Test fun openingSettingsKeepsTheSharedConversationActiveInTheVisibleCompanion() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        assumeTrue("Overlay permission must be granted on the dedicated test device",Settings.canDrawOverlays(context))
        lateinit var controller: CompanionController
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    controller=CompanionRuntime.acquire(activity,"persistence-test")
                    controller.autoSpeak=false
                    FloatingCompanionService.start(activity,controller,false)
                }
                for(i in 0..50) { if(FloatingCompanionService.running) break; Thread.sleep(100) }
                assertTrue(FloatingCompanionService.running)
                scenario.onActivity { controller.send("Ouvre paramètres") }
                Thread.sleep(700)
                instrumentation.runOnMainSync {
                    assertTrue("Leaving the activity must not stop the visible companion's session",controller.active)
                    assertTrue(FloatingCompanionService.running)
                    val other=CompanionRuntime.acquire(context,"same-session-test")
                    assertSame("The activity and overlay must share one controller/TTS instance",controller,other)
                    CompanionRuntime.release("same-session-test")
                    assertFalse(controller.status.startsWith("En pause"))
                }
            }
        } finally {
            instrumentation.runOnMainSync { FloatingCompanionService.stop(context); CompanionRuntime.release("persistence-test") }
        }
    }
}
