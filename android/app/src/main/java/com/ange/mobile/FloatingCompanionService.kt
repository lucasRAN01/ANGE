package com.ange.mobile

import android.Manifest
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.KeyguardManager
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
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
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
    private var voiceButton: ImageButton?=null
    private var params: WindowManager.LayoutParams?=null
    private var movement: ValueAnimator?=null
    private var roam=false
    private var nextWalk=0L
    private var screenOn=true
    private var microphoneArmed=false
    private var captureHidden=false
    private val screen=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action==Intent.ACTION_SCREEN_OFF) { screenOn=false; controller.stop(); root?.visibility=View.GONE; movement?.cancel() }
            else if(intent?.action==Intent.ACTION_USER_PRESENT || intent?.action==Intent.ACTION_SCREEN_ON) { updateVisibility() }
        }
    }
    private val pulse=object : Runnable {
        override fun run() {
            if(root==null) return
            updateVisibility()
            val action=controller.permissionAction ?: controller.pendingAction
            if(!MainActivity.visible && (action!=null || controller.contactChoices.isNotEmpty() || controller.appChoices.isNotEmpty())) openMain()
            val message=(if(controller.mood==CompanionMood.IDLE || controller.mood==CompanionMood.SPEAKING) controller.lastReply else controller.status).take(190)
            bubble?.apply { text=message; visibility=if(message.isBlank()) View.GONE else View.VISIBLE }
            voiceButton?.imageTintList=android.content.res.ColorStateList.valueOf(if(controller.mood==CompanionMood.LISTENING || controller.mood==CompanionMood.SPEAKING) Color.rgb(105,232,220) else Color.rgb(202,203,211))
            val now=android.os.SystemClock.uptimeMillis()
            if(roam && screenOn && controller.mood==CompanionMood.IDLE && now>nextWalk && movement==null) {
                nextWalk=now+22000; val p=params
                if(p!=null) moveTo(if(p.x<screenWidth()/2) screenWidth()-dp(OVERLAY_WIDTH+4) else dp(4),1600)
            }
            val p=params; val view=root
            if(screenOn && p!=null && view!=null) {
                val maxY=(screenHeight()-view.height-dp(30)).coerceAtLeast(dp(28))
                val x=p.x.coerceIn(0,(screenWidth()-dp(OVERLAY_WIDTH)).coerceAtLeast(0))
                val y=p.y.coerceIn(dp(28),maxY)
                if(p.x!=x || p.y!=y) { p.x=x; p.y=y; runCatching { windows.updateViewLayout(view,p) } }
            }
            handler.postDelayed(this,if(screenOn) 180 else 2000)
        }
    }
    override fun onCreate() {
        super.onCreate()
        controller=CompanionRuntime.acquire(this,"floating")
        instance=this
        windows=getSystemService(WINDOW_SERVICE) as WindowManager
        val filter=IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) }
        if(Build.VERSION.SDK_INT>=33) registerReceiver(screen,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(screen,filter)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action==STOP) { setDesired(this,false); controller.stop(); stopSelf(); return START_NOT_STICKY }
        if(intent==null && !getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("wanted",true)) { stopSelf(); return START_NOT_STICKY }
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
                microphoneArmed=intent!=null && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
                if(microphoneArmed) types=types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                try { startForeground(NOTIFICATION,notification,types) }
                catch (_: SecurityException) { microphoneArmed=false; startForeground(NOTIFICATION,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) }
            } else startForeground(NOTIFICATION,notification)
            if(Build.VERSION.SDK_INT<34) microphoneArmed=intent!=null && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
            if(intent==null) {
                controller.mode=OnDevicePolicy.safeMode(getSharedPreferences(PREFS,MODE_PRIVATE).getString("mode","ondevice"))
                roam=getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("roam",false)
            } else {
                roam=intent.getBooleanExtra("roam",false)
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("mode",controller.mode).putBoolean("roam",roam).apply()
            }
            controller.onForeground()
            if(root==null) show()
            running=true
        } catch (_: Exception) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }
    private fun updateVisibility() {
        val interactive=(getSystemService(POWER_SERVICE) as PowerManager).isInteractive
        val unlocked=!(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        val show=interactive && unlocked && !captureHidden
        if(show!=screenOn) {
            screenOn=show; root?.visibility=if(show) View.VISIBLE else View.GONE
            if(show) avatar?.restart() else { if(!interactive || !unlocked) controller.stop(); movement?.cancel() }
        }
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) { super.onConfigurationChanged(newConfig); handler.removeCallbacks(pulse); handler.post(pulse) }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun screenWidth()=resources.displayMetrics.widthPixels
    private fun screenHeight()=resources.displayMetrics.heightPixels
    private fun backdrop(): GradientDrawable = GradientDrawable().apply { setColor(Color.argb(238,12,22,43)); cornerRadius=dp(18).toFloat(); setStroke(dp(1),Color.rgb(73,200,224)) }
    private fun show() {
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES }
        root=layout
        bubble=TextView(this).apply { setTextColor(Color.WHITE); textSize=11f; maxLines=5; setPadding(dp(10),dp(8),dp(10),dp(8)); background=backdrop(); visibility=View.GONE }
        layout.addView(bubble,LinearLayout.LayoutParams(-1,-2))
        avatar=PhotoCompanionView(this).apply { controller=this@FloatingCompanionService.controller }
        layout.addView(avatar,LinearLayout.LayoutParams(-1,dp(116)))
        val buttons=LinearLayout(this).apply {
            gravity=Gravity.CENTER; elevation=dp(6).toFloat()
            background=GradientDrawable().apply { setColor(Color.argb(245,17,18,22)); cornerRadius=dp(24).toFloat() }
        }
        fun icon(resource: Int,description: String,click: (View) -> Unit): ImageButton {
            val button=ImageButton(this).apply {
                setImageResource(resource); contentDescription=description; tooltipText=description
                imageTintList=android.content.res.ColorStateList.valueOf(Color.rgb(202,203,211))
                setPadding(dp(11),dp(11),dp(11),dp(11))
                background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(50,255,255,255)),null,GradientDrawable().apply { shape=GradientDrawable.OVAL; setColor(Color.WHITE) })
                setOnClickListener { click(it) }
            }
            buttons.addView(button,LinearLayout.LayoutParams(0,dp(44),1f))
            return button
        }
        fun separator() { buttons.addView(View(this).apply { setBackgroundColor(Color.argb(65,160,162,174)) },LinearLayout.LayoutParams(dp(1),dp(17))) }
        icon(R.drawable.floating_open,"Ouvrir ANGE") { openMain() }
        separator()
        icon(R.drawable.floating_message,"Écrire un message à ANGE") { CompanionHandoff.openChat=true; openMain() }
        separator()
        voiceButton=icon(R.drawable.floating_voice,"Parler à ANGE") {
            if(MainActivity.visible) { CompanionHandoff.startVoice=true; openMain() }
            else if(microphoneArmed && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) controller.startListening()
            else { CompanionHandoff.startVoice=true; openMain() }
        }
        separator()
        icon(R.drawable.floating_more,"Options de la mini-ANGE") { anchor ->
            PopupMenu(this,anchor).apply {
                menu.add("Masquer les commandes").setOnMenuItemClickListener { buttons.visibility=View.GONE; true }
                menu.add("Fermer la mini-ANGE").setOnMenuItemClickListener { setDesired(this@FloatingCompanionService,false); controller.stop(); stopSelf(); true }
                show()
            }
        }
        avatar?.setOnClickListener { buttons.visibility=View.VISIBLE }
        layout.addView(buttons,LinearLayout.LayoutParams(-1,-2))
        params=WindowManager.LayoutParams(dp(OVERLAY_WIDTH),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=screenWidth()-dp(OVERLAY_WIDTH+4); y=dp(140) }
        var rawX=0f; var rawY=0f; var startX=0; var startY=0; var dragged=false
        avatar?.setOnTouchListener { view,event ->
            val p=params ?: return@setOnTouchListener false
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { movement?.cancel(); movement=null; rawX=event.rawX; rawY=event.rawY; startX=p.x; startY=p.y; dragged=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.rawX-rawX; val dy=event.rawY-rawY
                    if(abs(dx)+abs(dy)>dp(5)) dragged=true
                    p.x=(startX+dx.toInt()).coerceIn(0,(screenWidth()-dp(OVERLAY_WIDTH)).coerceAtLeast(0))
                    p.y=(startY+dy.toInt()).coerceIn(dp(28),(screenHeight()-layout.height-dp(30)).coerceAtLeast(dp(28)))
                    avatar?.moving=if(dx<0) -1 else 1; runCatching { windows.updateViewLayout(layout,p) }; true
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                    avatar?.moving=0
                    if(!dragged) view.performClick() else moveTo(if(p.x<screenWidth()/2) dp(4) else screenWidth()-dp(OVERLAY_WIDTH+4),350)
                    nextWalk=android.os.SystemClock.uptimeMillis()+22000; true
                }
                else -> false
            }
        }
        windows.addView(layout,params); nextWalk=android.os.SystemClock.uptimeMillis()+22000; handler.post(pulse)
    }
    private fun moveTo(target: Int,duration: Long) {
        val p=params ?: return; val view=root ?: return
        val destination=target.coerceIn(0,(screenWidth()-dp(OVERLAY_WIDTH)).coerceAtLeast(0))
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
        runCatching { unregisterReceiver(screen) }; CompanionRuntime.release("floating"); super.onDestroy()
    }
    companion object {
        private const val OVERLAY_WIDTH=180
        private var instance: FloatingCompanionService?=null
        fun hideForCapture(hidden: Boolean) { instance?.apply { captureHidden=hidden; updateVisibility() } }
        private const val PREFS="ange-floating"
        var desired by mutableStateOf(true); private set
        private fun setDesired(context: Context,value: Boolean) { desired=value; context.getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("wanted",value).apply() }
        private const val CHANNEL="ange-companion"
        private const val NOTIFICATION=802
        private const val STOP="com.ange.mobile.STOP_COMPANION"
        var running by mutableStateOf(false); private set
        fun start(context: Context,controller: CompanionController,roam: Boolean) {
            require(Settings.canDrawOverlays(context)) { "Autorisez d’abord l’affichage au-dessus des applications." }
            setDesired(context,true)
            context.startForegroundService(Intent(context,FloatingCompanionService::class.java).putExtra("mode",controller.mode)
                .putExtra("networkRecognition",controller.allowNetworkRecognition).putExtra("networkVoice",controller.allowNetworkVoice)
                .putExtra("onlineConsent",controller.onlineConsent).putExtra("roam",roam).putExtra("discussion",controller.handsFree))
        }
        fun restoreIfWanted(context: Context,controller: CompanionController) {
            desired=context.getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("wanted",true)
            if(desired && Settings.canDrawOverlays(context) && (!running || instance?.microphoneArmed==false)) start(context,controller,context.getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean("roam",false))
        }
        fun stop(context: Context) { setDesired(context,false); instance?.controller?.stop(); context.stopService(Intent(context,FloatingCompanionService::class.java)) }
    }
}
