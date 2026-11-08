package com.ange.mobile

import android.os.BatteryManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhoneFactsSmokeTest {
    private fun isolatedContext(base: Context): Context {
        val id = "phone-facts-test-${UUID.randomUUID()}"
        val directory = File(base.cacheDir, id).apply { mkdirs() }
        return object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir() = directory
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = base.getSharedPreferences("$id-$name", mode)
        }
    }

    @Test fun clockAnswersImmediatelyInEveryModeWithoutAModelOrServer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = isolatedContext(instrumentation.targetContext)
            val controller = CompanionController(context)
            try {
                controller.autoSpeak = false
                controller.language = "fr"
                controller.serverUrl = ""; controller.accessToken = ""
                assertFalse("No model is installed in the isolated context", controller.modelReady)
                for (mode in listOf("offline", "ondevice", "local")) {
                    controller.mode = mode
                    // Seed the exact failure reported by the user; facts must bypass it.
                    controller.lines.add(DialogueLine("assistant", "Je ne suis pas capable de connaître les heures.", mode))
                    val format = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.FRENCH)
                    val before = "Il est ${format.format(Date())}."
                    controller.send("quelle heure est-il")
                    val after = "Il est ${format.format(Date())}."
                    assertTrue("$mode: ${controller.lastReply}", controller.lastReply in setOf(before, after))
                    assertEquals("Téléphone · données système", controller.lines.last().engine)
                    assertEquals(CompanionMood.IDLE, controller.mood)
                    controller.send("quelle heure est-il")
                    assertTrue(controller.lastReply.startsWith("Il est "))
                    assertEquals(CompanionMood.IDLE, controller.mood)
                }
            } finally { controller.close() }
        }
    }

    @Test fun dateUsesTheAndroidCalendarInPhoneAiMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val controller = CompanionController(isolatedContext(instrumentation.targetContext))
            try {
                controller.autoSpeak = false; controller.language = "fr"; controller.mode = "ondevice"
                val format = DateFormat.getDateInstance(DateFormat.FULL, Locale.FRENCH)
                val before = "Nous sommes le ${format.format(Date())}."
                controller.send("Quel jour sommes-nous ?")
                val after = "Nous sommes le ${format.format(Date())}."
                assertTrue(controller.lastReply in setOf(before, after))
                assertEquals("Téléphone · données système", controller.lines.last().engine)
                assertEquals(CompanionMood.IDLE, controller.mood)
            } finally { controller.close() }
        }
    }

    @Test fun batteryReplyComesFromAndroidAndDoesNotNeedGenerativeAi() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = isolatedContext(instrumentation.targetContext)
            val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val controller = CompanionController(context)
            try {
                controller.autoSpeak = false; controller.language = "fr"; controller.mode = "ondevice"
                val before = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                controller.send("Combien de batterie me reste-t-il ?")
                val after = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                assertTrue(controller.lastReply.contains("à $before %") || controller.lastReply.contains("à $after %"))
                assertEquals("Téléphone · données système", controller.lines.last().engine)
                assertEquals(CompanionMood.IDLE, controller.mood)
            } finally { controller.close() }
        }
    }
}
