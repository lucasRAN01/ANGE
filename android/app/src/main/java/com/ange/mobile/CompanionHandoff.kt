package com.ange.mobile

/** Process-local handoff only; exported activity intents cannot inject commands. */
internal object CompanionHandoff {
    var action: DeviceAction? = null
    var contacts: List<ContactNumber> = emptyList()
    var startVoice = false
    var openChat = false
    fun clear() { action=null; contacts=emptyList(); startVoice=false; openChat=false }
}
