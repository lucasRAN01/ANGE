package com.ange.mobile

import org.junit.Assert.*
import org.junit.Test

class OnDevicePolicyTest {
    @Test fun freeModesExcludePaidCloud() {
        assertEquals("ondevice", OnDevicePolicy.safeMode("cloud"))
        assertEquals("ondevice", OnDevicePolicy.safeMode(null))
        assertEquals("local", OnDevicePolicy.safeMode("local"))
    }
    @Test fun pinnedModelOnly() {
        OnDevicePolicy.validateModel(OnDevicePolicy.MODEL_BYTES,OnDevicePolicy.MODEL_SHA256)
        try { OnDevicePolicy.validateModel(100,OnDevicePolicy.MODEL_SHA256); fail() } catch (_: IllegalArgumentException) {}
        try { OnDevicePolicy.validateModel(OnDevicePolicy.MODEL_BYTES,"0".repeat(64)); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun hiddenThinkingIsNeverDisplayed() {
        assertEquals("Bonjour",OnDevicePolicy.visibleReply("<think>secret\ntexte</think> Bonjour"))
        assertEquals("",OnDevicePolicy.visibleReply("<think>unfinished"))
        assertEquals("Bonjour",OnDevicePolicy.visibleReply("Bonjour<|im_end|>"))
    }
    @Test fun contextIsBounded() {
        val history=(1..40).map { "user" to "a".repeat(4000) }
        val prompt=OnDevicePolicy.prompt("b".repeat(4000),history,"fr")
        assertTrue(prompt.length < 1700)
        assertTrue(prompt.contains("Langue: fr"))
    }
}
