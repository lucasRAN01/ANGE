package com.ange.mobile

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class RobotRenderingSmokeTest {
    private fun find(view: View): RobotSurface? {
        if(view is RobotSurface) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun nativeSceneRendersRotatesAndPausesWithActivity() {
        val prefs=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("ange-companion",android.content.Context.MODE_PRIVATE)
        val previous=prefs.getString("avatarStyle","photo")
        prefs.edit().putString("avatarStyle","robot").commit()
        try {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val reference=AtomicReference<RobotSurface>()
            for(attempt in 0..30) {
                scenario.onActivity { reference.set(find(it.window.decorView)) }
                if(reference.get()?.robot?.renderedFrames?.let { it>3 }==true) break
                Thread.sleep(200)
            }
            val surface=reference.get()
            assertNotNull("The native 3D bot must be attached",surface)
            val renderer=surface!!.robot
            assertTrue("GL must draw several real frames",renderer.renderedFrames>3)
            assertEquals("OpenGL scene must render without errors",0,renderer.lastGlError)
            val before=renderer.renderedFrames
            scenario.onActivity { renderer.yaw=35f; renderer.mood=CompanionMood.SPEAKING; surface.requestRender() }
            Thread.sleep(250)
            assertTrue(renderer.renderedFrames>before)
            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(400); val paused=renderer.renderedFrames; Thread.sleep(300)
            assertEquals("GL rendering must stop in background",paused,renderer.renderedFrames)
            scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(400)
            assertTrue("GL must resume with foreground",renderer.renderedFrames>paused)
        }
        } finally { prefs.edit().putString("avatarStyle",previous).commit() }
    }
}
