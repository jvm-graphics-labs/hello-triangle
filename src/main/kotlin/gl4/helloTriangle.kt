package gl4

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH
import com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT
import com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT
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
import uno.debug.GlDebugOutput
import uno.glsl.Program
import java.nio.ByteBuffer
import kotlin.properties.Delegates

/**
 * Created by elect on 06/03/17.
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
        val MODEL_MATRIX = 3
        val MAX = 4
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vertexArrayName = intBufferBig(1)

    val clearColor = floatBufferBig(4)
    val clearDepth = floatBufferBig(1)

    val matBuffer = floatBufferBig(16)

    var globalMatricesPointer by Delegates.notNull<ByteBuffer>()
    var modelMatrixPointer by Delegates.notNull<ByteBuffer>()

    // https://jogamp.org/bugzilla/show_bug.cgi?id=1287
    val bug1287 = true

    var program by Delegates.notNull<Program>()

    var start = 0L


    fun initGL() {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window.title = "Hello Triangle (enhanced)"
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

        val gl = drawable.gl.gL4

        initDebug(gl)

        initBuffers(gl)

        initVertexArray(gl)

        program = Program(gl, this::class.java, "shaders/gl4", "hello-triangle.vert", "hello-triangle.frag")

        gl.glEnable(GL_DEPTH_TEST)

        start = System.currentTimeMillis()
    }

    fun initDebug(gl: GL4) = with(gl) {

        window.context.addGLDebugListener(GlDebugOutput())
        // Turn off all the debug
        glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DONT_CARE, // severity
                0, null, // id
                false)// count
        // enabled
        // Turn on all OpenGL Errors, shader compilation/linking errors, or highly-dangerous undefined behavior
        glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_HIGH,
                0, null,
                true)
        // Turn on all major performance warnings, shader compilation/linking warnings or the use of deprecated functions
        glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_MEDIUM,
                0, null,
                true)
    }

    fun initBuffers(gl: GL4) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val elementBuffer = elementData.toShortBuffer()

        glCreateBuffers(Buffer.MAX, bufferName)

        if (!bug1287) {

            glNamedBufferStorage(bufferName[Buffer.VERTEX], vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
            glNamedBufferStorage(bufferName[Buffer.ELEMENT], elementBuffer.SIZE.L, elementBuffer, GL_STATIC_DRAW)

            glNamedBufferStorage(bufferName[Buffer.GLOBAL_MATRICES], Mat4.SIZE.L * 2, null, GL_MAP_WRITE_BIT)
            glNamedBufferStorage(bufferName[Buffer.MODEL_MATRIX], Mat4.SIZE.L, null, GL_MAP_WRITE_BIT)

        } else {

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
            glBufferStorage(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, 0)
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
            glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.SIZE.L, elementBuffer, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)


            val uniformBufferOffset = intBufferBig(1)
            glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset)
            val globalBlockSize = glm.max(Mat4.SIZE * 2, uniformBufferOffset[0])
            val modelBlockSize = glm.max(Mat4.SIZE, uniformBufferOffset[0])

            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
            glBufferStorage(GL_UNIFORM_BUFFER, globalBlockSize.L, null, GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT)
            glBindBuffer(GL_UNIFORM_BUFFER, 0)

            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MODEL_MATRIX])
            glBufferStorage(GL_UNIFORM_BUFFER, modelBlockSize.L, null, GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT)
            glBindBuffer(GL_UNIFORM_BUFFER, 0)

            uniformBufferOffset.destroy()
        }

        destroyBuffers(vertexBuffer, elementBuffer)


        // map the transform buffers and keep them mapped
        globalMatricesPointer = glMapNamedBufferRange(
                bufferName[Buffer.GLOBAL_MATRICES], // buffer
                0, // offset
                Mat4.SIZE.L * 2, // size
                GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_INVALIDATE_BUFFER_BIT) // flags

        modelMatrixPointer = glMapNamedBufferRange(
                bufferName[Buffer.MODEL_MATRIX],
                0,
                Mat4.SIZE.L,
                GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_INVALIDATE_BUFFER_BIT)
    }

    fun initVertexArray(gl: GL4) = with(gl) {

        glCreateVertexArrays(1, vertexArrayName)

        glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.POSITION, Semantic.Stream.A)
        glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.COLOR, Semantic.Stream.A)

        glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, 0)
        glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.COLOR, Vec3.length, GL_FLOAT, false, Vec2.SIZE)

        glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.POSITION)
        glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.COLOR)

        glVertexArrayElementBuffer(vertexArrayName[0], bufferName[Buffer.ELEMENT])
        glVertexArrayVertexBuffer(vertexArrayName[0], Semantic.Stream.A, bufferName[Buffer.VERTEX], 0, Vec2.SIZE + Vec3.SIZE)
    }

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL4) {

        // update view matrix
        run {
            val view = Mat4()
            view.to(globalMatricesPointer, Mat4.SIZE)
        }


        glClearBufferfv(GL_COLOR, 0, clearColor.put(1f, .5f, 0f, 1f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f))

        run {
            // update model matrix based on time
            val now = System.currentTimeMillis()
            val diff = (now - start).f / 1_000f

            // Here we build the matrix that will multiply our original vertex positions. We scale, half and rotate it.
            val model = Mat4()
            model
                    .scale_(0.5f)
                    .rotate_(diff, 0f, 0f, 1f)
                    .to(modelMatrixPointer)
        }

        glUseProgram(program.name)
        glBindVertexArray(vertexArrayName[0])

        glBindBufferBase(
                GL_UNIFORM_BUFFER, // target
                Semantic.Uniform.TRANSFORM0, // index
                bufferName[Buffer.GLOBAL_MATRICES]) // buffer

        glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM1,
                bufferName[Buffer.MODEL_MATRIX])

        glDrawElements(
                GL_TRIANGLES, // primitive mode
                elementData.size, // element count
                GL_UNSIGNED_SHORT, // element type
                0) // element offset

        glUseProgram(0)
        glBindVertexArray(0)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = with(drawable.gl.gL4) {

        glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f) to globalMatricesPointer

        glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) = with(drawable.gl.gL4) {

        glUnmapNamedBuffer(bufferName[Buffer.GLOBAL_MATRICES])
        glUnmapNamedBuffer(bufferName[Buffer.MODEL_MATRIX])

        glDeleteProgram(program.name)
        glDeleteVertexArrays(1, vertexArrayName)
        glDeleteBuffers(Buffer.MAX, bufferName)

        destroyBuffers(vertexArrayName, bufferName, clearColor, clearDepth)
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            /* Note: calling System.exit() synchronously inside the draw, reshape or init callbacks can lead to
              deadlocks on certain platforms (in particular, X11) because the JAWT's locking routines cause a global
              AWT lock to be grabbed. Run the exit routine in another thread.  */
            Thread { window.destroy() }.start()
        }
    }

    override fun keyReleased(e: KeyEvent) {}
}