package com.ange.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Optional device test: provision the pinned model in target app files first; no model download here. */
@RunWith(AndroidJUnit4::class)
class PhoneInferenceSmokeTest {
    @Test fun offlineCpuInference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "qwen3-phone.litertlm")
        assertEquals("Provision the documented model first", OnDevicePolicy.MODEL_BYTES, file.length())
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer,0,n) }
        }
        assertEquals(OnDevicePolicy.MODEL_SHA256, digest.digest().joinToString("") { "%02x".format(it) })
        val invalid = File(context.cacheDir, "not-a-model.litertlm").apply { writeText("invalid model") }
        try {
            val error = runCatching { OnDeviceModelStore(context).import(android.net.Uri.fromFile(invalid)) {} }.exceptionOrNull()
            assertTrue("Corrupt import must be rejected", error is IllegalArgumentException)
            assertEquals("Rejected import must preserve existing model", OnDevicePolicy.MODEL_BYTES, file.length())
        } finally { invalid.delete() }
        val runner = OnDeviceRunner(context)
        try {
            val started = android.os.SystemClock.elapsedRealtime()
            val reply = runner.generate(file, OnDevicePolicy.prompt("Donne deux conseils courts pour organiser ma journée.", emptyList(), "fr"))
            assertTrue("Inference must produce a visible answer", reply.length > 10)
            assertFalse(reply.contains("<think>"))
            android.util.Log.i("ANGE-Phone-Test", "CPU reply in ${android.os.SystemClock.elapsedRealtime()-started} ms: $reply")
        } finally { runner.unload() }
    }
}
