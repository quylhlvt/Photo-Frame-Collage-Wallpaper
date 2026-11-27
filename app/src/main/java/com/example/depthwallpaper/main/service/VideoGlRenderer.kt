package com.example.depthwallpaper.main.service

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws decoded video frames onto the wallpaper surface itself, so we control how the picture is
 * placed: "fit" keeps the whole frame visible (like centerInside), "fill" crops to cover (like
 * centerCrop), and a rotation tag the phone's MediaPlayer ignores can be applied by hand.
 * MediaPlayer decodes into [videoSurface]; every new frame is drawn on a background GL thread.
 */
class VideoGlRenderer(private val target: Surface) {
    private val thread = HandlerThread("video-gl").apply { start() }
    private val handler = Handler(thread.looper)

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private var texId = 0
    private var posLoc = 0
    private var texLoc = 0
    private var texMatrixLoc = 0
    private var surfaceTexture: SurfaceTexture? = null

    lateinit var videoSurface: Surface
        private set

    @Volatile private var contentW = 0
    @Volatile private var contentH = 0
    @Volatile private var rotation = 0
    @Volatile private var fit = true
    @Volatile private var released = false

    private val stMatrix = FloatArray(16)
    private val quad = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val uv = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)); position(0)
    }

    init {
        val ready = CountDownLatch(1)
        handler.post {
            runCatching { setupGl() }
            ready.countDown()
        }
        ready.await()
    }

    /** Size of the frames as delivered, how much to rotate them by hand (0/90/180/270), and fit vs fill. */
    fun configure(width: Int, height: Int, rotationDegrees: Int, fit: Boolean) {
        contentW = width; contentH = height; rotation = rotationDegrees; this.fit = fit
        handler.post { if (!released) draw() }
    }

    private fun setupGl() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val v = IntArray(2)
        EGL14.eglInitialize(display, v, 0, v, 1)
        val cfg = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        EGL14.eglChooseConfig(
            display,
            intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE,
            ),
            0, cfg, 0, 1, n, 0,
        )
        context = EGL14.eglCreateContext(display, cfg[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        eglSurface = EGL14.eglCreateWindowSurface(display, cfg[0], target, intArrayOf(EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)

        program = buildProgram(
            "attribute vec4 aPos; attribute vec2 aTex; uniform mat4 uTex; varying vec2 vTex;" +
                "void main(){ gl_Position = aPos; vTex = (uTex * vec4(aTex, 0.0, 1.0)).xy; }",
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float; varying vec2 vTex; uniform samplerExternalOES sTex;" +
                "void main(){ gl_FragColor = texture2D(sTex, vTex); }",
        )
        posLoc = GLES20.glGetAttribLocation(program, "aPos")
        texLoc = GLES20.glGetAttribLocation(program, "aTex")
        texMatrixLoc = GLES20.glGetUniformLocation(program, "uTex")

        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)
        texId = t[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val st = SurfaceTexture(texId)
        st.setOnFrameAvailableListener({ if (!released) draw(true) }, handler)
        surfaceTexture = st
        videoSurface = Surface(st)
    }

    private fun draw(newFrame: Boolean = false) {
        val st = surfaceTexture ?: return
        if (display == EGL14.EGL_NO_DISPLAY) return
        runCatching {
            EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)
            if (newFrame) { st.updateTexImage(); st.getTransformMatrix(stMatrix) }
            val sw = IntArray(1); val sh = IntArray(1)
            EGL14.eglQuerySurface(display, eglSurface, EGL14.EGL_WIDTH, sw, 0)
            EGL14.eglQuerySurface(display, eglSurface, EGL14.EGL_HEIGHT, sh, 0)
            GLES20.glViewport(0, 0, sw[0], sh[0])
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (contentW > 0 && contentH > 0) {
                fillQuad(sw[0].toFloat(), sh[0].toFloat())
                GLES20.glUseProgram(program)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
                GLES20.glUniformMatrix4fv(texMatrixLoc, 1, false, stMatrix, 0)
                GLES20.glEnableVertexAttribArray(posLoc)
                GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, quad)
                GLES20.glEnableVertexAttribArray(texLoc)
                GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 0, uv)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            }
            EGL14.eglSwapBuffers(display, eglSurface)
        }
    }

    /** Corners of the frame in clip space after rotating by hand and scaling to fit/fill the screen. */
    private fun fillQuad(sw: Float, sh: Float) {
        val hw = contentW / 2f
        val hh = contentH / 2f
        val a = Math.toRadians(-rotation.toDouble()) // tag rotation is clockwise, GL is counter-clockwise
        val c = cos(a).toFloat(); val s = sin(a).toFloat()
        val corners = floatArrayOf(-hw, -hh, hw, -hh, -hw, hh, hw, hh)
        val rot = FloatArray(8)
        for (i in 0 until 4) {
            val x = corners[i * 2]; val y = corners[i * 2 + 1]
            rot[i * 2] = x * c - y * s
            rot[i * 2 + 1] = x * s + y * c
        }
        val dw = abs(hw * c) * 2 + abs(hh * s) * 2
        val dh = abs(hw * s) * 2 + abs(hh * c) * 2
        val k = if (fit) min(sw / dw, sh / dh) else max(sw / dw, sh / dh)
        quad.position(0)
        for (i in 0 until 4) {
            quad.put(rot[i * 2] * k / (sw / 2f))
            quad.put(rot[i * 2 + 1] * k / (sh / 2f))
        }
        quad.position(0)
    }

    private fun buildProgram(vs: String, fs: String): Int {
        fun shader(type: Int, src: String) = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src); GLES20.glCompileShader(it)
        }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        return p
    }

    fun release() {
        released = true
        val done = CountDownLatch(1)
        handler.post {
            runCatching { videoSurface.release() }
            runCatching { surfaceTexture?.release() }
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, eglSurface)
                EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
            }
            done.countDown()
        }
        done.await()
        thread.quitSafely()
    }
}
