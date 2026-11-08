package com.ange.mobile

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MiniRenderingSmokeTest {
    private fun find(view: View): PhotoCompanionView? {
        if(view is PhotoCompanionView) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun oneMiniIsVisibleAcrossAllTabsAndItsRenderingPausesWithTheApp() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val prefs=context.getSharedPreferences("ange-floating",Context.MODE_PRIVATE)
        val wanted=prefs.getBoolean("wanted",true)
        instrumentation.runOnMainSync { FloatingCompanionService.stop(context) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val reference=AtomicReference<PhotoCompanionView>()
                for(i in 0..30) {
                    scenario.onActivity { reference.set(find(it.window.decorView)) }
                    if((reference.get()?.renderedFrames ?: 0)>2) break
                    Thread.sleep(150)
                }
                val mini=reference.get(); assertNotNull("Only mini-ANGE should render",mini)
                for(tab in 0..3) {
                    scenario.onActivity { activity ->
                        val root=activity.window.decorView
                        val x=root.width*(tab+.5f)/4f
                        val y=root.height-55*activity.resources.displayMetrics.density
                        val now=android.os.SystemClock.uptimeMillis()
                        // Dispatch in this window: a previous test may leave Android Settings focused.
                        val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0)
                        val up=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,x,y,0)
                        try { root.dispatchTouchEvent(down); root.dispatchTouchEvent(up) }
                        finally { down.recycle(); up.recycle() }
                    }
                    Thread.sleep(200)
                    scenario.onActivity { activity ->
                        val current=find(activity.window.decorView)
                        assertNotNull("Mini-ANGE must remain on tab $tab",current)
                        assertTrue(current!!.height <= (116*activity.resources.displayMetrics.density+2).toInt())
                        assertTrue(current.isShown)
                    }
                }
                scenario.moveToState(Lifecycle.State.CREATED)
                Thread.sleep(300); val frames=mini!!.renderedFrames; Thread.sleep(250)
                assertEquals(frames,mini.renderedFrames)
                scenario.moveToState(Lifecycle.State.RESUMED); Thread.sleep(300)
                assertTrue(mini.renderedFrames>frames)
            }
        } finally { prefs.edit().putBoolean("wanted",wanted).commit() }
    }
}
