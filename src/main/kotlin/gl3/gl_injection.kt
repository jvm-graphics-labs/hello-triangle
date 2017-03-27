package gl3

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import glm.set
import uno.buffer.toFloatBuffer

/**
 * Created by GBarbieri on 27.03.2017.
 */

fun main(args: Array<String>) {
    GL_injection_().setup()
}

class GL_injection_ : GLEventListener, KeyListener {

    lateinit var window: GLWindow
    lateinit var animator:Animator
    
    val clearColor = floatArrayOf(0f, 0f, 0f, 0f).toFloatBuffer()

    fun setup() {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window.title = "gl injection"
        window.setSize(1024, 768)

        window.addGLEventListener(this)
        window.addKeyListener(this)

        window.autoSwapBufferMode = false

        window.isVisible = true

        animator = Animator(window)
        animator.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent?) {
                animator.stop()
                System.exit(1)
            }
        })
    }

    override fun init(drawable: GLAutoDrawable) {}

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {}

    override fun display(drawable: GLAutoDrawable) {}

    override fun dispose(drawable: GLAutoDrawable) {}

    override fun keyPressed(e: KeyEvent) {

        if (e.keyCode == KeyEvent.VK_ESCAPE)
            Thread { window.destroy() }.start()

        when (e.keyCode) {
            KeyEvent.VK_R -> modified(0)
            KeyEvent.VK_G -> modified(1)
            KeyEvent.VK_B -> modified(2)
            KeyEvent.VK_A -> modified(3)
        }
    }

    fun modified(index: Int) {
        clearColor[index] = if (clearColor[index] == 0f) 1f else 0f
        println("clear color: (" + clearColor[0] + ", " + clearColor[1] + ", " + clearColor[2] + ", " + clearColor[3] + ")")
        window.invoke(false) { drawable ->
            drawable.gl.gL3.glClearBufferfv(GL_COLOR, 0, clearColor)
            drawable.swapBuffers()
            true
        }
    }
    
    override fun keyReleased(e: KeyEvent) {}
}