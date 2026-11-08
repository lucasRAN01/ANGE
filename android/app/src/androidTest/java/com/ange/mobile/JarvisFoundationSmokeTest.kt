package com.ange.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class JarvisFoundationSmokeTest {
    // The instrumentation runs under target UID. Use an isolated cache directory,
    // never the user's real memory file (nor the inaccessible test-package UID).
    private val context by lazy {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val directory=File(target.cacheDir,"jarvis-tests-"+java.util.UUID.randomUUID()).apply { mkdirs() }
        object: android.content.ContextWrapper(target) { override fun getFilesDir(): File=directory }
    }
    @Test fun memoriesPersistSearchForgetAndEraseRecovery() {
        val store=JarvisMemoryStore(context); store.erase()
        try {
            store.remember("preferences","Aime le café","boisson")
            store.remember("facts","Projet ANGE"); store.remember("facts","Projet ANGE")
            assertEquals(2,JarvisMemoryStore(context).recall().size)
            assertEquals("Aime le café",store.recall("CAFE").single().text)
            assertEquals(1,store.forget("cafe"))
            assertTrue(File(context.filesDir,"jarvis-memory-recovery.json").exists())
            store.erase(); assertTrue(store.recall().isEmpty())
            assertFalse(File(context.filesDir,"jarvis-memory-recovery.json").exists())
        } finally { store.erase() }
    }
    @Test fun corruptMemoryIsNotSilentlyOverwritten() {
        val store=JarvisMemoryStore(context); store.erase()
        val file=File(context.filesDir,"jarvis-memory.json")
        try {
            file.writeText("{broken")
            try { store.remember("facts","New"); fail("Must retain corrupt memory") } catch(_: IllegalStateException) {}
            assertEquals("{broken",file.readText())
        } finally { store.erase() }
    }
    @Test fun sourceAndLicenseAreActuallyInsideApk() {
        val assets=InstrumentationRegistry.getInstrumentation().targetContext.assets
        val license=assets.open("licenses/jarvis-MIT.txt").bufferedReader().use { it.readText() }
        assertTrue(license.contains("Copyright (c) 2026 sosoj92"))
        val origin=assets.open("jarvis-origin.json").bufferedReader().use { it.readText() }
        assertTrue(origin.contains("15f3f87e0bfd3fe68ec38deafbfeab22636c3993"))
        assertFalse(JarvisVoice.spoken("Contact test@example.com",true).contains("test@example.com"))
        assertEquals("frederic",JarvisVoice.withoutAccents("Frédéric"))
    }
}
