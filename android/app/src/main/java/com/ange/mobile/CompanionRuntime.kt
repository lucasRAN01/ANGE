package com.ange.mobile

import android.content.Context

/** One conversation/TTS engine, retained while the visible companion owns the session. */
internal object CompanionRuntime {
    private var controller: CompanionController?=null
    private val owners=mutableSetOf<String>()
    fun acquire(context: Context,owner: String): CompanionController {
        owners.add(owner)
        return controller ?: CompanionController(context.applicationContext).also { controller=it }
    }
    fun release(owner: String) {
        owners.remove(owner)
        if(owners.isEmpty()) { controller?.close(); controller=null }
        else if(owner=="floating" && !MainActivity.visible) controller?.onBackground()
    }
}
