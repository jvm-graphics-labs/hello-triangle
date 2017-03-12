package gl3

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.util.Animator
import framework.Semantic
import glm.L
import glm.SIZE
import glm.f
import glm.glm
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._3.Vec3
import uno.buffer.*
import uno.gl.checkError
import uno.glsl.Program
import kotlin.properties.Delegates

/**
 * Created by elect on 05/03/17.
 */

fun main(args: Array<String>) {
    HelloTriangleK().initGL()
}

class HelloTriangleK : GLEventListener, KeyListener {

    var window by Delegates.notNull<GLWindow>()
    val animator = Animator()

    val vertexData = floatArrayOf(
            -1f, -1f, 1f, 0f, 0f,
            +0f, +2f, 0f, 0f, 1f,
            +1f, -1f, 0f, 1f, 0f)

    val elementData = shortArrayOf(0, 2, 1)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val GLOBAL_MATRICES = 2
        val MAX = 3
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vertexArrayName = intBufferBig(1)

    val clearColor = floatBufferBig(4)
    val clearDepth = floatBufferBig(1)

    val matBuffer = floatBufferBig(16)

    var program by Delegates.notNull<Program>()

    var start = 0L

    fun initGL() {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window.title = "Hello Triangle"
        window.setSize(1024, 768)

        window.isVisible = true

        window.addGLEventListener(this)
        window.addKeyListener(this)

        animator.add(window)
        animator.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent?) {
                animator.stop(); System.exit(1); }
        })
    }

    override fun init(drawable: GLAutoDrawable) {

        val gl = drawable.gl.gL3

        initBuffers(gl)

        initVertexArray(gl)

        initProgram(gl)

        gl.glEnable(GL_DEPTH_TEST)

        start = System.currentTimeMillis()
    }

    fun initBuffers(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val elementBuffer = elementData.toShortBuffer()

        glGenBuffers(Buffer.MAX, bufferName)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.SIZE.L, elementBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)


        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE.L * 2, null, GL_STREAM_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, bufferName[Buffer.GLOBAL_MATRICES])


        destroyBuffers(vertexBuffer, elementBuffer)

        checkError(gl, "initBuffers")
    }

    fun initVertexArray(gl: GL3) = with(gl) {

        glGenVertexArrays(1, vertexArrayName)
        glBindVertexArray(vertexArrayName[0])

        run {

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])

            run {
                val stride = Vec2.SIZE + Vec3.SIZE
                var offset = 0

                glEnableVertexAttribArray(Semantic.Attr.POSITION)
                glVertexAttribPointer(Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, stride, offset.L)

                offset = Vec2.SIZE
                glEnableVertexAttribArray(Semantic.Attr.COLOR)
                glVertexAttribPointer(Semantic.Attr.COLOR, Vec3.length, GL_FLOAT, false, stride, offset.L)
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        }
        glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    fun initProgram(gl: GL3) = with(gl) {

        program = Program(gl, this::class.java, "shaders/gl3", "hello-triangle.vert", "hello-triangle.frag", "model")

        val globalMatricesBI = glGetUniformBlockIndex(program.name, "GlobalMatrices")

        if (globalMatricesBI == -1) {
            System.err.println("block index 'GlobalMatrices' not found!")
        }
        glUniformBlockBinding(program.name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES)

        checkError(gl, "initProgram")
    }

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        // view matrix
        run {
            val view = Mat4()

            view to matBuffer

            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
            glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, Mat4.SIZE.L, matBuffer)
            glBindBuffer(GL_UNIFORM_BUFFER, 0)
        }

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0f, .33f, 0.66f, 1f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f))

        glUseProgram(program.name)
        glBindVertexArray(vertexArrayName[0])

        // model matrix
        run {
            val now = System.currentTimeMillis()
            val diff = (now - start).f / 1_000f

            val model = Mat4()
                    .scale(0.5f)
                    .rotate(diff, 0f, 0f, 1f)

            model to matBuffer

            glUniformMatrix4fv(program["model"], 1, false, matBuffer)
        }

        glDrawElements(GL_TRIANGLES, elementData.size, GL_UNSIGNED_SHORT, 0)

        glUseProgram(0)
        glBindVertexArray(0)

        checkError(drawable.gl, "display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = with(drawable.gl.gL3) {

        val ortho = glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f)

        ortho to matBuffer

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE.L, matBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        glDeleteProgram(program.name)
        glDeleteVertexArrays(1, vertexArrayName)
        glDeleteBuffers(Buffer.MAX, bufferName)

        destroyBuffers(vertexArrayName, bufferName, matBuffer, clearColor, clearDepth)
    }


    override fun keyPressed(e: KeyEvent) {

        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            Thread(Runnable { window.destroy() }).start()
        }
    }

    override fun keyReleased(e: KeyEvent) {}
}