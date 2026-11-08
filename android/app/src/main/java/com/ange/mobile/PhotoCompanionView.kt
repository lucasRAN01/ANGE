package com.ange.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

internal object PhotoMotion {
    fun frame(mood: CompanionMood, millis: Long, beat: Int, moving: Int, waving: Boolean, animated: Boolean): Int {
        if(!animated) return 0
        if(moving<0) return 8 + ((millis/220)%2).toInt()
        if(moving>0) return 10 + ((millis/220)%2).toInt()
        if(waving) return if((millis/400)%3==0L) 7 else 6
        return when(mood) {
            CompanionMood.LISTENING -> 4
            CompanionMood.THINKING -> 5
            CompanionMood.SPEAKING -> if((millis/170+beat)%3==0L) 0 else 2+((millis/170+beat)%2).toInt()
            CompanionMood.ERROR -> 5
            CompanionMood.IDLE -> if(millis%5100 in 4600..4730) 1 else 0
        }
    }
}

/** Twelve rendered poses. This is a 2D sprite companion, not a rigged 3D human. */
class PhotoCompanionView(context: Context) : View(context) {
    private val atlas: Bitmap = atlas(context)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var controller: CompanionController? = null
    var moving=0
    var wavingUntil=0L
    var renderedFrames=0; private set
    var currentPose=0; private set
    private val tick=object : Runnable {
        override fun run() {
            if(isAttachedToWindow && isShown && controller?.active!=false) { invalidate(); postDelayed(this,80) }
        }
    }
    init { contentDescription="Mini-compagne ANGE inspirée de votre photo"; importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); restart() }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) { super.onWindowVisibilityChanged(visibility); if(visibility==VISIBLE) restart() else removeCallbacks(tick) }
    fun restart() { removeCallbacks(tick); post(tick); invalidate() }
    fun wave() { wavingUntil=SystemClock.uptimeMillis()+1800; restart() }
    override fun performClick(): Boolean { wave(); return super.performClick() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now=SystemClock.uptimeMillis()
        val state=controller
        val animated=(state?.animations!=false && state?.active!=false && android.animation.ValueAnimator.areAnimatorsEnabled())
        currentPose=PhotoMotion.frame(state?.mood ?: CompanionMood.IDLE,now,state?.mouthBeat ?: 0,moving,now<wavingUntil,animated)
        val col=currentPose%4; val row=currentPose/4
        val source=Rect(col*atlas.width/4,row*atlas.height/3,(col+1)*atlas.width/4,(row+1)*atlas.height/3)
        val h=height*.96f
        val w=(h*source.width()/source.height()).coerceAtMost(width.toFloat())
        val fittedH=w*source.height()/source.width()
        val dy=if(animated) sin(now/900.0).toFloat()*height*.004f else 0f
        val dest=RectF((width-w)/2f,height-fittedH+dy,(width+w)/2f,height.toFloat()+dy)
        canvas.drawBitmap(atlas,source,dest,paint)
        renderedFrames++
    }
    companion object {
        private var cached: Bitmap? = null
        @Synchronized private fun atlas(context: Context): Bitmap = cached ?: BitmapFactory.decodeResource(context.resources,R.drawable.ange_photo_atlas).also { cached=it }
    }
}
