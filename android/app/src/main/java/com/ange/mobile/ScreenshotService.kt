package com.ange.mobile

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotService : Service() {
    private val main=Handler(Looper.getMainLooper())
    private var projection: MediaProjection?=null
    private var display: VirtualDisplay?=null
    private var reader: ImageReader?=null
    private val finished=AtomicBoolean(false)
    private lateinit var worker: HandlerThread
    override fun onCreate() { super.onCreate(); worker=HandlerThread("ange-capture").apply { start() } }
    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code=intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("resultData",Intent::class.java) else intent?.getParcelableExtra("resultData") as? Intent
        if(code!=Activity.RESULT_OK || data==null) { stopSelf(); return START_NOT_STICKY }
        try {
            val notifications=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notifications.createNotificationChannel(NotificationChannel("ange-capture","Captures demandées",NotificationManager.IMPORTANCE_LOW))
            val notification=Notification.Builder(this,"ange-capture").setSmallIcon(R.drawable.ui_camera).setContentTitle("ANGE · capture demandée").setContentText("Enregistrement local d’une image autorisée par Android").setOngoing(true).build()
            if(Build.VERSION.SDK_INT>=29) startForeground(803,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(803,notification)
            projection=(getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,data)
            projection?.registerCallback(object : MediaProjection.Callback() { override fun onStop() { finishCapture("Capture interrompue par Android.") } },main)
            main.postDelayed({ createCapture() },400)
            main.postDelayed({ finishCapture("Aucune image reçue. La capture a été arrêtée.") },8000)
        } catch (_: Exception) { finishCapture("Android a refusé la capture. Aucune image envoyée ni enregistrée.") }
        return START_NOT_STICKY
    }
    @Suppress("DEPRECATION")
    private fun createCapture() {
        if(finished.get()) return
        try {
            val metrics=DisplayMetrics()
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val width=metrics.widthPixels; val height=metrics.heightPixels
            val images=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2)
            reader=images
            images.setOnImageAvailableListener({ source ->
                val image=source.acquireLatestImage() ?: return@setOnImageAvailableListener
                if(!finished.compareAndSet(false,true)) { image.close(); return@setOnImageAvailableListener }
                try {
                    val plane=image.planes[0]
                    val paddedWidth=width+(plane.rowStride-plane.pixelStride*width)/plane.pixelStride
                    val padded=Bitmap.createBitmap(paddedWidth,height,Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(plane.buffer)
                    val bitmap=Bitmap.createBitmap(padded,0,0,width,height)
                    try { save(bitmap) } finally { if(bitmap!==padded) bitmap.recycle(); padded.recycle() }
                    main.post { announceAndStop("Capture enregistrée dans Images / ANGE sur le téléphone.") }
                } catch (_: Exception) { main.post { announceAndStop("La capture n’a pas pu être enregistrée.") } }
                finally { image.close() }
            },Handler(worker.looper))
            display=projection?.createVirtualDisplay("ANGE capture demandée",width,height,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,images.surface,null,main)
        } catch (_: Exception) { finishCapture("Impossible de lire l’image de capture autorisée.") }
    }
    private fun save(bitmap: Bitmap) {
        val name="ANGE-${System.currentTimeMillis()}.png"
        if(Build.VERSION.SDK_INT>=29) {
            val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/png"); put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ANGE"); put(MediaStore.Images.Media.IS_PENDING,1) }
            val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Galerie indisponible")
            try {
                contentResolver.openOutputStream(uri)?.use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } ?: error("Stockage indisponible")
                contentResolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)
            } catch(error: Exception) { contentResolver.delete(uri,null,null); throw error }
        } else {
            val folder=File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"ANGE").apply { mkdirs() }
            File(folder,name).outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        }
    }
    private fun finishCapture(message: String) { if(finished.compareAndSet(false,true)) announceAndStop(message) }
    private fun announceAndStop(message: String) {
        val controller=CompanionRuntime.acquire(this,"capture-result")
        controller.deviceResult(message); CompanionRuntime.release("capture-result"); stopSelf()
    }
    override fun onBind(intent: Intent?): IBinder?=null
    override fun onDestroy() {
        finished.set(true); main.removeCallbacksAndMessages(null)
        display?.release(); reader?.close(); projection?.stop(); worker.quitSafely()
        FloatingCompanionService.hideForCapture(false)
        super.onDestroy()
    }
}
