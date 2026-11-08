package com.ange.mobile

import android.Manifest
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** Visible user-started companion; no boot receiver, no hidden or continuous listening. */
class FloatingCompanionService : Service() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var controller: CompanionController
    private lateinit var windows: WindowManager
    private var root: LinearLayout?=null
    private var avatar: PhotoCompanionView?=null
    private var bubble: TextView?=null
    private var params: WindowManager.LayoutParams?=null
    private var movement: ValueAnimator?=null
    private var roam=false
    private var discussion=false
    private var nextWalk=0L
    private var screenOn=true
    private val screen=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action==Intent.ACTION_SCREEN_OFF) { screenOn=false; controller.stop(); root?.visibility=View.GONE; movement?.cancel() }
            else if(intent?.action==Intent.ACTION_USER_PRESENT) { screenOn=true; root?.visibility=View.VISIBLE; avatar?.restart() }
        }
    }
    private val pulse=object : Runnable {
        override fun run() {
            if(root==null) return
            val action=controller.permissionAction ?: controller.pendingAction
            if(action!=null || controller.contactChoices.isNotEmpty()) {
                CompanionHandoff.action=action; CompanionHandoff.contacts=controller.contactChoices.toList()
                controller.stop(); openMain()
            }
            bubble?.text=(if(controller.mood==CompanionMood.IDLE || controller.mood==CompanionMood.SPEAKING) controller.lastReply.ifBlank { "Touchez Parler pour me demander une action." } else controller.status).take(190)
            val now=android.os.SystemClock.uptimeMillis()
            if(roam && screenOn && controller.mood==CompanionMood.IDLE && now>nextWalk && movement==null) {
                nextWalk=now+22000; val p=params
                if(p!=null) moveTo(if(p.x<screenWidth()/2) screenWidth()-dp(152) else dp(4),1600)
            }
            val p=params; val view=root
            if(screenOn && p!=null && view!=null) {
                val maxY=(screenHeight()-view.height-dp(30)).coerceAtLeast(dp(28))
                if(p.y>maxY) { p.y=maxY; runCatching { windows.updateViewLayout(view,p) } }
            }
            handler.postDelayed(this,if(screenOn) 180 else 2000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        controller=CompanionController(this)
        instance=this
        windows=getSystemService(WINDOW_SERVICE) as WindowManager
        val filter=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }
        if(Build.VERSION.SDK_INT>=33) registerReceiver(screen,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(screen,filter)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action==STOP) { stopSelf(); return START_NOT_STICKY }
        if(!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val manager=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Mini-compagne ANGE",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,FloatingCompanionService::class.java).setAction(STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ui_voice).setContentTitle("ANGE est à vos côtés")
            .setContentText("Mini-compagne visible · micro sur appui · Arrêter pour fermer")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"Arrêter",stop).build()).build()
        try {
            if(Build.VERSION.SDK_INT>=34) {
                var types=ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) types=types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                startForeground(NOTIFICATION,notification,types)
            } else startForeground(NOTIFICATION,notification)
            controller.mode=OnDevicePolicy.safeMode(intent?.getStringExtra("mode"))
            controller.setNetworkRecognitionAllowed(intent?.getBooleanExtra("networkRecognition",false)==true)
            controller.allowNetworkVoice=intent?.getBooleanExtra("networkVoice",false)==true
            controller.onlineConsent=intent?.getBooleanExtra("onlineConsent",false)==true
            roam=intent?.getBooleanExtra("roam",false)==true
            discussion=intent?.getBooleanExtra("discussion",false)==true
            if(root==null) show()
            running=true
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun screenWidth()=resources.displayMetrics.widthPixels
    private fun screenHeight()=resources.displayMetrics.heightPixels
    private fun backdrop(): GradientDrawable = GradientDrawable().apply { setColor(Color.argb(238,12,22,43)); cornerRadius=dp(18).toFloat(); setStroke(dp(1),Color.rgb(73,200,224)) }
    private fun show() {
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES }
        root=layout
        bubble=TextView(this).apply { setTextColor(Color.WHITE); textSize=11f; maxLines=5; setPadding(dp(10),dp(8),dp(10),dp(8)); background=backdrop() }
        layout.addView(bubble,LinearLayout.LayoutParams(-1,-2))
        avatar=PhotoCompanionView(this).apply { controller=this@FloatingCompanionService.controller }
        layout.addView(avatar,LinearLayout.LayoutParams(-1,dp(186)))
        val buttons=LinearLayout(this).apply { gravity=Gravity.CENTER; background=backdrop() }
        fun button(label: String, click: () -> Unit) { buttons.addView(Button(this).apply { text=label; textSize=10f; minWidth=0; minimumWidth=0; isAllCaps=false; setTextColor(Color.rgb(105,232,220)); backgroundTintList=android.content.res.ColorStateList.valueOf(Color.rgb(20,48,65)); setPadding(0,0,0,0); setOnClickListener { click() } },LinearLayout.LayoutParams(0,dp(42),1f)) }
        button("Parler") {
            if(MainActivity.visible) { CompanionHandoff.startVoice=true; openMain() }
            else if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) { if(discussion) controller.enableHandsFree(); controller.startListening() }
            else { CompanionHandoff.startVoice=true; openMain() }
        }
        button("ANGE") { openMain() }
        button("×") { stopSelf() }
        layout.addView(buttons,LinearLayout.LayoutParams(-1,-2))
        params=WindowManager.LayoutParams(dp(148),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=screenWidth()-dp(152); y=dp(140) }
        var rawX=0f; var rawY=0f; var startX=0; var startY=0; var dragged=false
        avatar?.setOnTouchListener { view,event ->
            val p=params ?: return@setOnTouchListener false
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { movement?.cancel(); movement=null; rawX=event.rawX; rawY=event.rawY; startX=p.x; startY=p.y; dragged=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.rawX-rawX; val dy=event.rawY-rawY
                    if(abs(dx)+abs(dy)>dp(5)) dragged=true
                    p.x=(startX+dx.toInt()).coerceIn(0,(screenWidth()-dp(148)).coerceAtLeast(0))
                    p.y=(startY+dy.toInt()).coerceIn(dp(28),(screenHeight()-layout.height-dp(30)).coerceAtLeast(dp(28)))
                    avatar?.moving=if(dx<0) -1 else 1; runCatching { windows.updateViewLayout(layout,p) }; true
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                    avatar?.moving=0
                    if(!dragged) view.performClick() else moveTo(if(p.x<screenWidth()/2) dp(4) else screenWidth()-dp(152),350)
                    nextWalk=android.os.SystemClock.uptimeMillis()+22000; true
                }
                else -> false
            }
        }
        windows.addView(layout,params); nextWalk=android.os.SystemClock.uptimeMillis()+22000; handler.post(pulse)
    }
    private fun moveTo(target: Int,duration: Long) {
        val p=params ?: return; val view=root ?: return
        val destination=target.coerceIn(0,(screenWidth()-dp(148)).coerceAtLeast(0))
        avatar?.moving=if(destination<p.x) -1 else 1
        movement=ValueAnimator.ofInt(p.x,destination).apply {
            this.duration=duration
            addUpdateListener { p.x=it.animatedValue as Int; runCatching { windows.updateViewLayout(view,p) } }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { avatar?.moving=0; movement=null }
            }); start()
        }
    }
    private fun openMain() { startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        running=false; handler.removeCallbacksAndMessages(null); movement?.cancel()
        if(instance===this) instance=null
        root?.let { runCatching { windows.removeView(it) } }; root=null
        runCatching { unregisterReceiver(screen) }; controller.close(); super.onDestroy()
    }
    companion object {
        private var instance: FloatingCompanionService?=null
        fun pauseConversation() { instance?.controller?.stop(false) }
        private const val CHANNEL="ange-companion"
        private const val NOTIFICATION=802
        private const val STOP="com.ange.mobile.STOP_COMPANION"
        var running by mutableStateOf(false); private set
        fun start(context: Context,controller: CompanionController,roam: Boolean) {
            require(Settings.canDrawOverlays(context)) { "Autorisez d’abord l’affichage au-dessus des applications." }
            context.startForegroundService(Intent(context,FloatingCompanionService::class.java).putExtra("mode",controller.mode)
                .putExtra("networkRecognition",controller.allowNetworkRecognition).putExtra("networkVoice",controller.allowNetworkVoice)
                .putExtra("onlineConsent",controller.onlineConsent).putExtra("roam",roam).putExtra("discussion",controller.handsFree))
        }
        fun stop(context: Context) { context.stopService(Intent(context,FloatingCompanionService::class.java)) }
    }
}
