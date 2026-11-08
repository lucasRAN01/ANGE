package com.ange.mobile

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Explicit navigation only. No screen logging, remote upload or arbitrary clicks. */
class AngeNavigationService : AccessibilityService() {
    override fun onServiceConnected() { instance=this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if(instance===this) instance=null; super.onDestroy() }
    private fun scroll(node: AccessibilityNodeInfo?, forward: Boolean, depth: Int = 0): Boolean {
        if(node==null || depth>30) return false
        if(node.isScrollable && node.performAction(if(forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true
        for(i in 0 until node.childCount) if(scroll(node.getChild(i),forward,depth+1)) return true
        return false
    }
    companion object {
        @Volatile private var instance: AngeNavigationService? = null
        fun enabled() = instance != null
        fun navigate(direction: String): Boolean {
            val service=instance ?: return false
            return when(direction) {
                "retour" -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                "accueil" -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                "récentes" -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "descendre" -> service.scroll(service.rootInActiveWindow,true)
                "monter" -> service.scroll(service.rootInActiveWindow,false)
                else -> false
            }
        }
    }
}
