package com.ange.mobile

import android.content.Context
import android.opengl.GLES20 as GL
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** Original procedural 3D robot, rendered locally. No web view, downloaded model or raster impostor. */
@Composable
fun RobotCompanion3D(controller: CompanionController) {
    val context=LocalContext.current
    val systemMotion=remember { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f }
    val view=remember(context) { RobotSurface(context).apply { robot.onFailure={ message -> post { controller.rendererFailed(message) } } } }
    DisposableEffect(view,context) {
        val lifecycle=(context as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_STOP || event==Lifecycle.Event.ON_DESTROY) view.suspendRendering()
            if(event==Lifecycle.Event.ON_START) view.updateRendering(true,controller.animations && systemMotion)
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer); view.suspendRendering() }
    }
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        AndroidView(factory={ view },modifier=Modifier.fillMaxWidth().height(300.dp)
            .border(1.dp,Color(0x5531CFFF),RoundedCornerShape(18.dp)),update={
            it.robot.mood=controller.mood
            it.robot.mouthBeat=controller.mouthBeat
            it.robot.microphone=controller.microphoneLevel
            it.onGreeting={ controller.greet() }
            it.updateRendering(controller.active,controller.animations && systemMotion)
        })
        Text("ANGE · Bot 3D local · Glissez horizontalement pour le tourner, touchez pour saluer.",fontSize=10.sp,color=Color(0xFF8BEAFF))
    }
}

class RobotSurface(context: Context) : GLSurfaceView(context) {
    val robot=RobotRenderer()
    var onGreeting: () -> Unit = {}
    private var paused=false
    private var downX=0f; private var downY=0f; private var previousX=0f
    private var dragged=false
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,16,0)
        preserveEGLContextOnPause=true
        setRenderer(robot)
        contentDescription="ANGE, robot 3D animé. Glissez horizontalement pour tourner, touchez pour une salutation."
        setOnClickListener { robot.waveUntil=SystemClock.elapsedRealtime()+2200; onGreeting(); requestRender() }
    }
    fun updateRendering(active: Boolean, animated: Boolean) {
        robot.animated=animated
        if(!active) { suspendRendering(); return }
        if(paused) { onResume(); paused=false }
        renderMode=if(animated) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
        requestRender()
    }
    fun suspendRendering() { if(!paused) { onPause(); paused=true } }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX=event.x; downY=event.y; previousX=event.x; dragged=false; return true }
            MotionEvent.ACTION_MOVE -> {
                if(abs(event.x-downX)>12f && abs(event.x-downX)>abs(event.y-downY)) {
                    dragged=true; parent?.requestDisallowInterceptTouchEvent(true)
                    robot.yaw=(robot.yaw+(event.x-previousX)*.22f).coerceIn(-50f,50f); requestRender()
                }
                previousX=event.x; return true
            }
            MotionEvent.ACTION_UP -> { if(!dragged && abs(event.y-downY)<18f) performClick(); parent?.requestDisallowInterceptTouchEvent(false); return true }
            MotionEvent.ACTION_CANCEL -> { parent?.requestDisallowInterceptTouchEvent(false); return true }
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean = super.performClick()
}

class RobotRenderer : GLSurfaceView.Renderer {
    @Volatile var mood=CompanionMood.IDLE
    @Volatile var mouthBeat=0
    @Volatile var microphone=0f
    @Volatile var animated=true
    @Volatile var yaw=-8f
    @Volatile var waveUntil=0L
    @Volatile var renderedFrames=0L; private set
    @Volatile var lastGlError=0; private set
    @Volatile var onFailure: (String)->Unit = {}
    private var failed=false
    private var program=0
    private var position=0; private var normal=0; private var mvpLocation=0; private var modelLocation=0
    private var normalLocation=0; private var colorLocation=0; private var glowLocation=0
    private lateinit var sphere: Mesh
    private lateinit var torus: Mesh
    private lateinit var cylinder: Mesh
    private val projection=FloatArray(16); private val camera=FloatArray(16)
    private val model=FloatArray(16); private val world=FloatArray(16); private val head=FloatArray(16)
    private val temp=FloatArray(16); private val mvp=FloatArray(16); private val inverse=FloatArray(16)
    private val normalMatrix=FloatArray(16)
    private var nextFrame=0L
    private val origin=SystemClock.elapsedRealtime()
    private val pearl=floatArrayOf(.72f,.83f,.92f,1f)
    private val dark=floatArrayOf(.018f,.04f,.075f,1f)
    private val steel=floatArrayOf(.07f,.16f,.24f,1f)
    private val cyan=floatArrayOf(.015f,.66f,1f,1f)
    private val white=floatArrayOf(.72f,.96f,1f,1f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        failed=false
        try { initializeScene() } catch(error: Exception) { failed=true; onFailure(error.message ?: "OpenGL indisponible") }
    }
    private fun initializeScene() {
        val vertex="""
            uniform mat4 uMvp; uniform mat4 uModel; uniform mat4 uNormal;
            attribute vec3 aPosition; attribute vec3 aNormal;
            varying vec3 vPosition; varying vec3 vNormal;
            void main(){ vec4 p=uModel*vec4(aPosition,1.0); vPosition=p.xyz;
              vNormal=normalize((uNormal*vec4(aNormal,0.0)).xyz); gl_Position=uMvp*vec4(aPosition,1.0); }
        """
        val fragment="""
            precision mediump float;
            uniform vec4 uColor; uniform float uGlow; varying vec3 vPosition; varying vec3 vNormal;
            void main(){ vec3 n=normalize(vNormal); vec3 l=normalize(vec3(-3.0,5.0,5.0));
              vec3 eye=normalize(vec3(0.0,0.55,5.1)-vPosition); vec3 h=normalize(l+eye);
              float diffuse=max(dot(n,l),0.0); float spec=pow(max(dot(n,h),0.0),60.0);
              float rim=pow(1.0-max(dot(n,eye),0.0),3.0);
              vec3 lit=uColor.rgb*(0.20+0.80*diffuse)+vec3(0.60,0.80,1.0)*spec*0.65+vec3(0.0,0.38,0.66)*rim*0.6;
              gl_FragColor=vec4(mix(lit,uColor.rgb*1.18,clamp(uGlow,0.0,1.0)),uColor.a); }
        """
        fun shader(type: Int, source: String): Int {
            val id=GL.glCreateShader(type); GL.glShaderSource(id,source); GL.glCompileShader(id)
            val result=IntArray(1); GL.glGetShaderiv(id,GL.GL_COMPILE_STATUS,result,0)
            check(result[0]!=0) { GL.glGetShaderInfoLog(id) }; return id
        }
        val vs=shader(GL.GL_VERTEX_SHADER,vertex); val fs=shader(GL.GL_FRAGMENT_SHADER,fragment)
        program=GL.glCreateProgram(); GL.glAttachShader(program,vs); GL.glAttachShader(program,fs); GL.glLinkProgram(program)
        val linked=IntArray(1); GL.glGetProgramiv(program,GL.GL_LINK_STATUS,linked,0)
        check(linked[0]!=0) { GL.glGetProgramInfoLog(program) }
        GL.glDeleteShader(vs); GL.glDeleteShader(fs)
        position=GL.glGetAttribLocation(program,"aPosition"); normal=GL.glGetAttribLocation(program,"aNormal")
        mvpLocation=GL.glGetUniformLocation(program,"uMvp"); modelLocation=GL.glGetUniformLocation(program,"uModel")
        normalLocation=GL.glGetUniformLocation(program,"uNormal"); colorLocation=GL.glGetUniformLocation(program,"uColor"); glowLocation=GL.glGetUniformLocation(program,"uGlow")
        sphere=Mesh.sphere(); torus=Mesh.torus(); cylinder=Mesh.cylinder()
        GL.glEnable(GL.GL_DEPTH_TEST)
        GL.glClearColor(.018f,.035f,.072f,1f)
        Matrix.setLookAtM(camera,0,0f,.55f,5.1f,0f,.15f,0f,0f,1f,0f)
    }
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GL.glViewport(0,0,width,height)
        val ratio=width.toFloat()/height.coerceAtLeast(1)
        Matrix.perspectiveM(projection,0,36f,ratio,.1f,30f)
    }
    override fun onDrawFrame(gl: GL10?) {
        if(failed) { GL.glClear(GL.GL_COLOR_BUFFER_BIT); return }
        val now=SystemClock.elapsedRealtime()
        if(animated && nextFrame>now) Thread.sleep((nextFrame-now).coerceAtMost(33))
        nextFrame=SystemClock.elapsedRealtime()+33
        GL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT); GL.glUseProgram(program)
        val t=if(animated) (SystemClock.elapsedRealtime()-origin)/1000f else 0f
        val bob=if(animated) sin(t*1.8f)*.045f else 0f
        Matrix.setIdentityM(world,0); Matrix.translateM(world,0,0f,bob,0f)
        Matrix.rotateM(world,0,yaw+if(animated) sin(t*.65f)*3f else 0f,0f,1f,0f)
        // Low floating plinth and orbital halo, behind the body.
        part(torus,floatArrayOf(.03f,.25f,.40f,1f),0f,-1.21f,0f,1.02f,1.02f,1.02f,1f,90f,1f,0f,0f)
        part(torus,cyan,0f,-1.20f,0f,.88f,.88f,.88f,1f,90f,1f,0f,0f)
        part(torus,steel,0f,.62f,-.39f,1.10f,1.10f,.6f,0f,-18f,0f,0f,1f)
        // Torso, neck and articulated hips.
        part(sphere,steel,0f,-.40f,0f,.51f,.64f,.34f)
        part(sphere,pearl,0f,-.32f,.13f,.42f,.51f,.28f)
        part(sphere,dark,0f,-.67f,.31f,.25f,.17f,.055f)
        part(sphere,cyan,0f,-.67f,.365f,.12f,.045f,.025f,1f)
        part(cylinder,steel,0f,.21f,0f,.17f,.21f,.17f)
        for(side in listOf(-1f,1f)) {
            part(sphere,steel,side*.23f,-.97f,-.01f,.19f,.23f,.20f)
            part(sphere,pearl,side*.25f,-1.02f,.10f,.18f,.13f,.29f)
            part(sphere,cyan,side*.25f,-1.02f,.365f,.12f,.025f,.015f,1f)
            arm(side,t,now)
        }
        // Three dimensional orbital A insignia on the chest.
        part(cylinder,cyan,-.065f,-.28f,.405f,.023f,.16f,.023f,1f,-23f,0f,0f,1f)
        part(cylinder,cyan,.065f,-.28f,.405f,.023f,.16f,.023f,1f,23f,0f,0f,1f)
        part(cylinder,white,0f,-.32f,.43f,.018f,.10f,.018f,.8f,90f,0f,0f,1f)
        part(torus,cyan,0f,-.28f,.42f,.21f,.13f,.12f,1f,-22f,0f,0f,1f)
        System.arraycopy(world,0,head,0,16)
        Matrix.translateM(head,0,0f,.73f,0f)
        Matrix.rotateM(head,0,when(mood) { CompanionMood.LISTENING -> -10f; CompanionMood.THINKING -> 7f; else -> if(animated) sin(t*.9f)*2f else 0f },0f,0f,1f)
        Matrix.rotateM(head,0,if(mood==CompanionMood.SPEAKING && animated) sin(t*3f)*3f else 0f,1f,0f,0f)
        part(sphere,pearl,0f,0f,0f,.69f,.60f,.48f,parent=head)
        part(sphere,steel,0f,.02f,.24f,.62f,.47f,.30f,parent=head)
        part(sphere,dark,0f,.025f,.31f,.57f,.405f,.275f,parent=head)
        val blink=if(animated && t%4.9f>.0f && t%4.9f<.13f) .08f else 1f
        val attentive=if(mood==CompanionMood.LISTENING) 1.12f else 1f
        for(side in listOf(-1f,1f)) {
            part(sphere,cyan,side*.235f,.08f,.555f,.125f,.125f*blink*attentive,.035f,1f,parent=head)
            part(sphere,white,side*.215f,.114f,.588f,.028f,.030f*blink,.010f,1f,parent=head)
            part(sphere,steel,side*.69f,.025f,0f,.13f,.23f,.22f,parent=head)
            part(torus,cyan,side*.775f,.025f,.015f,.17f,.17f,.17f,1f,90f,0f,1f,0f,head)
            part(sphere,cyan,side*.57f,-.20f,.38f,.018f,.035f,.018f,.8f,parent=head)
        }
        val mouth=if(mood==CompanionMood.SPEAKING && animated) .035f+abs(sin(t*14f+mouthBeat))*.08f else .025f
        for(i in -2..2) {
            val height=if(mood==CompanionMood.LISTENING) .03f+microphone*.055f*(3-abs(i)) else mouth*(1f-abs(i)*.16f)
            part(sphere,cyan,i*.055f,-.175f,.575f,.017f,height,.012f,1f,parent=head)
        }
        part(cylinder,steel,0f,.59f,-.05f,.035f,.12f,.035f,parent=head)
        val indicator=if(mood==CompanionMood.THINKING && animated) .6f+.4f*sin(t*5f) else 1f
        part(sphere,cyan,0f,.73f,-.05f,.085f,.085f,.085f,indicator,parent=head)
        renderedFrames++; lastGlError=GL.glGetError()
        if(lastGlError!=GL.GL_NO_ERROR) { failed=true; onFailure("Erreur graphique $lastGlError") }
    }
    private fun arm(side: Float,t: Float,now: Long) {
        val transform=world.copyOf()
        Matrix.translateM(transform,0,side*.52f,-.12f,0f)
        val wave=side>0 && now<waveUntil && animated
        val angle=if(wave) 125f+sin(t*7f)*16f else 13f+if(animated) sin(t*1.8f)*2f else 0f
        Matrix.rotateM(transform,0,side*angle,0f,0f,1f)
        part(sphere,steel,0f,0f,0f,.18f,.18f,.19f,parent=transform)
        part(sphere,pearl,0f,-.23f,0f,.14f,.27f,.15f,parent=transform)
        part(torus,cyan,0f,-.42f,0f,.125f,.125f,.125f,1f,90f,1f,0f,0f,transform)
        part(sphere,steel,0f,-.51f,0f,.125f,.16f,.13f,parent=transform)
        part(sphere,pearl,0f,-.65f,.01f,.13f,.12f,.12f,parent=transform)
        for(i in -1..1) part(sphere,pearl,i*.07f,-.76f,.025f,.031f,.09f,.04f,parent=transform)
    }
    private fun part(mesh: Mesh,color: FloatArray,x: Float,y: Float,z: Float,sx: Float,sy: Float,sz: Float,glow: Float=0f,
        rotation: Float=0f,rx: Float=0f,ry: Float=0f,rz: Float=1f,parent: FloatArray=world) {
        System.arraycopy(parent,0,model,0,16); Matrix.translateM(model,0,x,y,z)
        if(rotation!=0f) Matrix.rotateM(model,0,rotation,rx,ry,rz)
        Matrix.scaleM(model,0,sx,sy,sz)
        Matrix.multiplyMM(temp,0,camera,0,model,0); Matrix.multiplyMM(mvp,0,projection,0,temp,0)
        Matrix.invertM(inverse,0,model,0); Matrix.transposeM(normalMatrix,0,inverse,0)
        GL.glUniformMatrix4fv(mvpLocation,1,false,mvp,0); GL.glUniformMatrix4fv(modelLocation,1,false,model,0)
        GL.glUniformMatrix4fv(normalLocation,1,false,normalMatrix,0); GL.glUniform4fv(colorLocation,1,color,0); GL.glUniform1f(glowLocation,glow)
        mesh.draw(position,normal)
    }
    private class Mesh(vertices: FloatArray) {
        private val buffer: FloatBuffer=ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
        private val count=vertices.size/6
        fun draw(position: Int,normal: Int) {
            buffer.position(0); GL.glVertexAttribPointer(position,3,GL.GL_FLOAT,false,24,buffer); GL.glEnableVertexAttribArray(position)
            buffer.position(3); GL.glVertexAttribPointer(normal,3,GL.GL_FLOAT,false,24,buffer); GL.glEnableVertexAttribArray(normal)
            GL.glDrawArrays(GL.GL_TRIANGLES,0,count)
        }
        companion object {
            private fun surface(rows: Int,columns: Int,point: (Float,Float)->FloatArray): Mesh {
                val vertices=ArrayList<Float>()
                for(row in 0 until rows) for(col in 0 until columns) {
                    val a=point(row.toFloat()/rows,col.toFloat()/columns); val b=point((row+1f)/rows,col.toFloat()/columns)
                    val c=point((row+1f)/rows,(col+1f)/columns); val d=point(row.toFloat()/rows,(col+1f)/columns)
                    for(p in listOf(a,b,c,a,c,d)) for(v in p) vertices.add(v)
                }
                return Mesh(vertices.toFloatArray())
            }
            fun sphere()=surface(28,40) { u,v ->
                val theta=u*PI.toFloat(); val phi=v*2*PI.toFloat()
                val x=sin(theta)*cos(phi); val y=cos(theta); val z=sin(theta)*sin(phi)
                floatArrayOf(x,y,z,x,y,z)
            }
            fun torus()=surface(12,48) { u,v ->
                val a=u*2*PI.toFloat(); val b=v*2*PI.toFloat(); val radius=.055f
                floatArrayOf((1+radius*cos(a))*cos(b),(1+radius*cos(a))*sin(b),radius*sin(a),cos(a)*cos(b),cos(a)*sin(b),sin(a))
            }
            fun cylinder()=surface(1,32) { u,v ->
                val a=v*2*PI.toFloat(); floatArrayOf(cos(a),u*2-1,sin(a),cos(a),0f,sin(a))
            }
        }
    }
}
