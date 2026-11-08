package com.ange.mobile

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Companion23SmokeTest {
    private class TestContext(base: Context): ContextWrapper(base) {
        val id="companion23-${UUID.randomUUID()}"
        val directory=File(base.cacheDir,id).apply { mkdirs() }
        val launched=mutableListOf<Intent>()
        override fun getApplicationContext(): Context=this
        override fun getFilesDir()=directory
        override fun getSharedPreferences(name: String,mode: Int): SharedPreferences=baseContext.getSharedPreferences("$id-$name",mode)
        override fun checkSelfPermission(permission: String)=if(permission in setOf(Manifest.permission.READ_CONTACTS,Manifest.permission.CALL_PHONE)) PackageManager.PERMISSION_GRANTED else super.checkSelfPermission(permission)
        override fun startActivity(intent: Intent) { launched.add(intent) } // Never actually call anyone in tests.
    }
    @Test fun namedContactBecomesARealCallIntentOnlyAfterConfirmation() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=TestContext(instrumentation.targetContext)
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(context) { _,name -> ContactDirectory.match(name,listOf(ContactNumber("Babe","5550100"))) }
            controller.autoSpeak=false; controller.send("Appelle Babe")
        }
        try {
            val ready=java.util.concurrent.atomic.AtomicBoolean(false)
            for(i in 0..50) { instrumentation.runOnMainSync { ready.set(controller.pendingAction!=null) }; if(ready.get()) break; Thread.sleep(100) }
            instrumentation.runOnMainSync {
                assertEquals(DeviceAction.CallNumber("5550100","Babe"),controller.pendingAction)
                assertTrue(context.launched.isEmpty())
                controller.confirmAction()
                assertEquals(Intent.ACTION_CALL,context.launched.single().action)
                assertEquals("tel:5550100",context.launched.single().dataString)
            }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
    @Test fun ambiguousContactsMustBeChosenAndCancelNeverStartsACall() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=TestContext(instrumentation.targetContext)
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(context) { _,_ -> listOf(ContactNumber("Babe","5550100"),ContactNumber("Babe","5550101")) }
            controller.autoSpeak=false; controller.send("Appelle Babe")
        }
        try {
            val ready=java.util.concurrent.atomic.AtomicBoolean(false)
            for(i in 0..50) { instrumentation.runOnMainSync { ready.set(controller.contactChoices.isNotEmpty()) }; if(ready.get()) break; Thread.sleep(100) }
            instrumentation.runOnMainSync {
                assertEquals(2,controller.contactChoices.size); assertNull(controller.pendingAction)
                controller.chooseContact(controller.contactChoices[1]); assertEquals(DeviceAction.CallNumber("5550101","Babe"),controller.pendingAction)
                controller.cancelAction(); assertTrue(context.launched.isEmpty())
            }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
    @Test fun taughtAliasPersistsAndStillRequiresCallConfirmation() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context=TestContext(instrumentation.targetContext)
            val controller=CompanionController(context)
            try {
                controller.autoSpeak=false
                controller.teachRoutine("Mon appel test","Appelle 5550100")
                assertEquals("Appelle 5550100",RoutineStore(context).resolve("Mon appel test"))
                controller.send("Mon appel test")
                assertTrue(controller.pendingAction is DeviceAction.CallNumber)
                assertTrue(context.launched.isEmpty()); controller.cancelAction()
                controller.teachRoutine("Exécute du code","supprime tous mes fichiers")
                assertNull(RoutineStore(context).resolve("Exécute du code"))
                controller.removeRoutine("Mon appel test"); assertTrue(RoutineStore(context).all().isEmpty())
            } finally { controller.close() }
        }
    }
    @Test fun onlinePayloadIsBoundedAndDoesNotExecuteModelOutput() {
        val history=(1..30).map { "user" to "a".repeat(3000) }
        val data=OnlineConversation.payload("Une question",history,"fr")
        assertEquals(13,data.getJSONArray("contents").length())
        assertEquals(1500,data.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text").length)
        assertFalse(data.has("tools"))
        val reply=OnlineConversation.reply(JSONObject("""{"candidates":[{"content":{"parts":[{"thought":true,"text":"internal"},{"text":"Bonjour"}]}}]}"""))
        assertEquals("Bonjour",reply)
        assertTrue(runCatching { OnlineConversation.reply(JSONObject("{}")) }.isFailure)
    }
    @Test fun photoAtlasHasTransparentEdgesAndTwelveReadableCells() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap=android.graphics.BitmapFactory.decodeResource(context.resources,R.drawable.ange_photo_atlas)
        assertTrue(bitmap.width>=1000); assertTrue(bitmap.height>=1000)
        assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(0,0)))
        for(frame in 0..11) {
            val x=(frame%4*bitmap.width/4)+bitmap.width/8
            val y=(frame/4*bitmap.height/3)+bitmap.height/6
            assertTrue("Pose $frame must contain character pixels",android.graphics.Color.alpha(bitmap.getPixel(x,y))>200)
        }
        bitmap.recycle()
    }
    @Test fun voiceConfirmationRequiresAPendingCallAndAnExplicitPhrase() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context=TestContext(instrumentation.targetContext)
            val controller=CompanionController(context)
            try {
                controller.autoSpeak=false; controller.mode="offline"
                controller.send("Confirme l’appel"); assertTrue(context.launched.isEmpty())
                controller.send("Appelle 5550100"); assertNotNull(controller.pendingAction)
                controller.send("Annule"); assertNull(controller.pendingAction); assertTrue(context.launched.isEmpty())
                controller.send("Appelle 5550100"); controller.send("Confirme l’appel")
                assertEquals(1,context.launched.size); assertEquals(Intent.ACTION_CALL,context.launched.single().action)
            } finally { controller.close() }
        }
    }
    @Test fun onlineModeWithoutSessionConsentCannotSendAQuestionButFactsStillWork() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val controller=CompanionController(TestContext(instrumentation.targetContext))
            try {
                controller.autoSpeak=false; controller.mode="online"; controller.onlineKey="not-a-real-key-for-test"; controller.onlineConsent=false
                controller.send("Parlons de ma journée")
                assertEquals(CompanionMood.ERROR,controller.mood); assertEquals("",controller.lastReply)
                assertTrue(controller.status.contains("autorisez les conversations en ligne"))
                controller.send("Quelle heure est-il ?")
                assertTrue(controller.lastReply.startsWith("Il est ")); assertEquals("Téléphone · données système",controller.lines.last().engine)
            } finally { controller.close() }
        }
    }
    @Test fun onlineControllerSendsOnlyConversationAndExcludesCallsAndOldPhoneFacts() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val captured=java.util.concurrent.atomic.AtomicReference<JSONObject>()
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(TestContext(instrumentation.targetContext),onlineGenerate={ _,payload,_ -> captured.set(payload); "Réponse simulée sans requête réseau." })
            controller.autoSpeak=false; controller.mode="online"; controller.onlineConsent=true; controller.onlineKey="not-a-real-key-for-test"
            controller.send("Appelle 5550100")
            controller.send("Quelle heure est-il ?")
            controller.send("Bonjour, parlons de ma journée.")
        }
        try {
            for(i in 0..50) { if(captured.get()!=null) break; Thread.sleep(100) }
            val payload=captured.get(); assertNotNull(payload)
            assertEquals(1,payload!!.getJSONArray("contents").length())
            assertFalse(payload.toString().contains("5550100")); assertFalse(payload.toString().contains("Il est "))
            assertTrue(payload.toString().contains("parlons de ma journée"))
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
    @Test fun spokenShortNotesNameOpensTheInstalledAppWithoutAskingAModel() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=TestContext(instrumentation.targetContext)
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(context,installedApps={ listOf(LaunchableApp("com.android.settings","Samsung Notes")) })
            controller.autoSpeak=false; controller.mode="ondevice"
        }
        try {
            val ready=java.util.concurrent.atomic.AtomicBoolean(false)
            for(i in 0..50) { instrumentation.runOnMainSync { ready.set(controller.availableApps.isNotEmpty()) }; if(ready.get()) break; Thread.sleep(100) }
            instrumentation.runOnMainSync {
                controller.send("Ouvre Notes")
                assertEquals("com.android.settings",context.launched.single().component!!.packageName)
                assertTrue(controller.lastReply.contains("Samsung Notes")); assertEquals(CompanionMood.IDLE,controller.mood)
            }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
    @Test fun appNameAmbiguityWaitsForAFullNameBeforeOpening() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=TestContext(instrumentation.targetContext)
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(context,installedApps={ listOf(LaunchableApp("com.android.settings","Samsung Notes"),LaunchableApp("com.example.other","Notes privées")) })
            controller.autoSpeak=false
        }
        try {
            val ready=java.util.concurrent.atomic.AtomicBoolean(false)
            for(i in 0..50) { instrumentation.runOnMainSync { ready.set(controller.availableApps.isNotEmpty()) }; if(ready.get()) break; Thread.sleep(100) }
            instrumentation.runOnMainSync {
                controller.send("Ouvre Notes")
                assertEquals(2,controller.appChoices.size); assertTrue(context.launched.isEmpty())
                controller.send("Samsung Notes")
                assertTrue(controller.appChoices.isEmpty()); assertEquals("com.android.settings",context.launched.single().component!!.packageName)
            }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
    @Test fun missingAppRefreshesTheInstalledListOnceBeforeReportingFailure() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=TestContext(instrumentation.targetContext)
        val scans=java.util.concurrent.atomic.AtomicInteger(0)
        lateinit var controller: CompanionController
        instrumentation.runOnMainSync {
            controller=CompanionController(context,installedApps={ if(scans.incrementAndGet()==1) emptyList() else listOf(LaunchableApp("com.android.settings","Samsung Notes")) })
            controller.autoSpeak=false
        }
        try {
            for(i in 0..50) { if(scans.get()>0) break; Thread.sleep(100) }
            instrumentation.runOnMainSync { controller.send("Ouvre Notes") }
            val opened=java.util.concurrent.atomic.AtomicBoolean(false)
            for(i in 0..50) { instrumentation.runOnMainSync { opened.set(context.launched.isNotEmpty()) }; if(opened.get()) break; Thread.sleep(100) }
            instrumentation.runOnMainSync { assertTrue(opened.get()); assertEquals(2,scans.get()); assertEquals("com.android.settings",context.launched.single().component!!.packageName) }
        } finally { instrumentation.runOnMainSync { controller.close() } }
    }
}
