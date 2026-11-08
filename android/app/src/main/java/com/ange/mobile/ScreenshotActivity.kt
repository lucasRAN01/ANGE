package com.ange.mobile

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Android asks for a fresh capture consent every time. No cached capture tokens. */
class ScreenshotActivity : ComponentActivity() {
    private var captureStarted=false
    private val consent=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode==Activity.RESULT_OK && result.data!=null) {
            try {
                startForegroundService(Intent(this,ScreenshotService::class.java).putExtra("resultCode",result.resultCode).putExtra("resultData",result.data))
                captureStarted=true
            } catch (_: Exception) { Toast.makeText(this,"La capture n’a pas pu démarrer.",Toast.LENGTH_LONG).show() }
        } else Toast.makeText(this,"Capture annulée. Aucune image enregistrée.",Toast.LENGTH_SHORT).show()
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FloatingCompanionService.hideForCapture(true)
        if(savedInstanceState==null) {
            val manager=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            consent.launch(if(Build.VERSION.SDK_INT>=34) manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()) else manager.createScreenCaptureIntent())
        }
    }
    override fun onDestroy() { if(!captureStarted) FloatingCompanionService.hideForCapture(false); super.onDestroy() }
}
