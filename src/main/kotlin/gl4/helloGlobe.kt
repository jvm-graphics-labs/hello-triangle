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
import com.jogamp.opengl.util.texture.TextureIO
import framework.Semantic
import glm.*
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import uno.buffer.*
import uno.debug.GlDebugOutput
import uno.glsl.Program
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.properties.Delegates

fun main(args: Array<String>) {
    HelloGlobe_().setup()
}

class HelloGlobe_ : GLEventListener, KeyListener {

    var window by Delegates.notNull<GLWindow>()
    val animator = Animator()

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val GLOBAL_MATRICES = 2
        val MODEL_MATRIX = 3
        val MAX = 4
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vertexArrayName = intBufferBig(1)

    val textureName = intBufferBig(1)
    val samplerName = intBufferBig(1)

    val clearColor = floatBufferBig(Vec4.length)
    val clearDepth = floatBufferBig(1)

    var globalMatricesPointer by Delegates.notNull<ByteBuffer>()
    var modelMatrixPointer by Delegates.notNull<ByteBuffer>()

    // https://jogamp.org/bugzilla/show_bug.cgi?id=1287
    val bug1287 = true

    var program by Delegates.notNull<Program>()

    var start = 0L

    var elementCount = 0

    fun setup() {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window.title = "Hello Globe"
        window.setSize(1024, 768)

        window.contextCreationFlags = GLContext.CTX_OPTION_DEBUG
        window.isVisible = true

        window.addGLEventListener(this)
        window.addKeyListener(this)

        animator.add(window)
        animator.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent?) {
                animator.stop()
                System.exit(1)
            }
        })
    }

    override fun init(drawable: GLAutoDrawable) {

        val gl = drawable.gl.gL4

        initDebug(gl)

        initBuffers(gl)

        initTexture(gl)

        initSampler(gl)

        initVertexArray(gl)

        program = Program(gl, this::class.java, "shaders/gl4", "hello-globe.vert", "hello-globe.frag")

        gl.glEnable(GL.GL_DEPTH_TEST)

        start = System.currentTimeMillis()
    }

    fun initDebug(gl: GL4) = with(gl) {

        window.context.addGLDebugListener(GlDebugOutput())

        glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DONT_CARE,
                0, null,
                false)

        glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_HIGH,
                0, null,
                true)

        glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_MEDIUM,
                0, null,
                true)
    }

    fun initBuffers(gl: GL4) = with(gl) {

        val radius = 1f
        val rings = 100.s
        val sectors = 100.s

        val vertexBuffer = getVertexBuffer(radius, rings, sectors)
        val elementBuffer = getElementBuffer(radius, rings, sectors)

        elementCount = elementBuffer.capacity()

        glCreateBuffers(Buffer.MAX, bufferName)

        if (!bug1287) {

            glNamedBufferStorage(bufferName[Buffer.VERTEX], vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
            glNamedBufferStorage(bufferName[Buffer.ELEMENT], elementBuffer.SIZE.L, elementBuffer, GL_STATIC_DRAW)

            glNamedBufferStorage(bufferName[Buffer.GLOBAL_MATRICES], Mat4.SIZE * 2.L, null, GL_MAP_WRITE_BIT)
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
                bufferName[Buffer.GLOBAL_MATRICES],
                0,
                Mat4.SIZE * 2.L,
                GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_INVALIDATE_BUFFER_BIT)

        modelMatrixPointer = glMapNamedBufferRange(
                bufferName[Buffer.MODEL_MATRIX],
                0,
                Mat4.SIZE.L,
                GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_INVALIDATE_BUFFER_BIT)
    }

    fun getVertexBuffer(radius: Float, rings: Short, sectors: Short): FloatBuffer {

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()
        var r: Short = 0
        var s: Short
        var x: Float
        var y: Float
        var z: Float

        val vertexBuffer = floatBufferBig(rings.toInt() * sectors.toInt() * (3 + 2))

        while (r < rings) {

            s = 0
            while (s < sectors) {

                x = glm.cos(2f * glm.pi.toFloat() * s.toFloat() * S) * glm.sin(glm.pi.toFloat() * r.toFloat() * R)
                y = glm.sin(-glm.pi.toFloat() / 2 + glm.pi.toFloat() * r.toFloat() * R)
                z = glm.sin(2f * glm.pi.toFloat() * s.toFloat() * S) * glm.sin(glm.pi.toFloat() * r.toFloat() * R)
                // positions
                vertexBuffer.put(x * radius)
                vertexBuffer.put(y * radius)
                vertexBuffer.put(z * radius)
                // texture coordinates
                vertexBuffer.put(1 - s * S)
                vertexBuffer.put(r * R)
                s++
            }
            r++
        }
        vertexBuffer.position(0)

        return vertexBuffer
    }

    fun getElementBuffer(radius: Float, rings: Short, sectors: Short): ShortBuffer {

        val R = 1f / (rings - 1).f
        val S = 1f / (sectors - 1).f
        var r = 0.s
        var s: Short
        val x: Float
        val y: Float
        val z: Float

        val elementBuffer = shortBufferBig(rings.i * sectors.i * 6)

        while (r < rings - 1) {

            s = 0
            while (s < sectors - 1) {

                elementBuffer.put((r * sectors + s).s)
                elementBuffer.put((r * sectors + (s + 1)).s)
                elementBuffer.put(((r + 1) * sectors + (s + 1)).s)
                elementBuffer.put(((r + 1) * sectors + (s + 1)).s)
                elementBuffer.put((r * sectors + s).s)
                //                elementBuffer.put((short) (r * sectors + (s + 1)));
                elementBuffer.put(((r + 1) * sectors + s).s)
                s++
            }
            r++
        }
        elementBuffer.position(0)

        return elementBuffer
    }

    fun initVertexArray(gl: GL4) = with(gl) {

        glCreateVertexArrays(1, vertexArrayName)

        glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.POSITION, Semantic.Stream.A)
        glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.TEXCOORD, Semantic.Stream.A)

        glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.POSITION, Vec3.length, GL_FLOAT, false, 0)
        glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.TEXCOORD, Vec2.length, GL_FLOAT, false, Vec3.SIZE)

        glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.POSITION)
        glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.TEXCOORD)

        glVertexArrayElementBuffer(vertexArrayName[0], bufferName[Buffer.ELEMENT])

        glVertexArrayVertexBuffer(vertexArrayName[0], Semantic.Stream.A, bufferName[Buffer.VERTEX], 0, Vec2.SIZE + Vec3.SIZE)
    }

    fun initTexture(gl: GL4) = with(gl) {

        val texture = this::class.java.classLoader.getResource("images/globe.png")

        val textureData = TextureIO.newTextureData(glProfile, texture!!, false, TextureIO.PNG)

        glCreateTextures(GL_TEXTURE_2D, 1, textureName)

        glTextureParameteri(textureName[0], GL_TEXTURE_BASE_LEVEL, 0)
        glTextureParameteri(textureName[0], GL_TEXTURE_MAX_LEVEL, 0)

        glTextureStorage2D(textureName[0],
                1, // level
                textureData.internalFormat,
                textureData.width, textureData.height)

        glTextureSubImage2D(textureName.get(0),
                0, // level
                0, 0, // offset
                textureData.width, textureData.height,
                textureData.pixelFormat, textureData.pixelType,
                textureData.buffer)
    }

    fun initSampler(gl: GL4) = with(gl) {

        glGenSamplers(1, samplerName)

        glSamplerParameteri(samplerName[0], GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glSamplerParameteri(samplerName[0], GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL4) {

        // view matrix
        run {
            val view = glm.lookAt(Vec3(0f, 0f, 3f), Vec3(), Vec3(0f, 1f, 0f))
            view.to(globalMatricesPointer, Mat4.SIZE)
        }

        glClearBufferfv(GL_COLOR, 0, clearColor.put(1f, .5f, 0f, 1f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f))


        // model matrix
        run {
            val now = System.currentTimeMillis()
            val diff = (now - start).f / 1_000f

            val model = Mat4().rotate_(-diff, 0f, 1f, 0f)
            model to modelMatrixPointer
        }

        glUseProgram(program.name)
        glBindVertexArray(vertexArrayName[0])

        glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM0,
                bufferName[Buffer.GLOBAL_MATRICES])

        glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM1,
                bufferName[Buffer.MODEL_MATRIX])

        glBindTextureUnit(
                Semantic.Sampler.DIFFUSE,
                textureName[0])
        glBindSampler(Semantic.Sampler.DIFFUSE, samplerName[0])

        glDrawElements(
                GL.GL_TRIANGLES,
                elementCount,
                GL_UNSIGNED_SHORT,
                0)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {

        val gl = drawable.gl.gL4

        val aspect = width.f / height

        val proj = glm.perspective(glm.pi.f * 0.25f, aspect, 0.1f, 100f)
        proj to globalMatricesPointer
    }

    override fun dispose(drawable: GLAutoDrawable) = with(drawable.gl.gL4) {

        glUnmapNamedBuffer(bufferName[Buffer.GLOBAL_MATRICES])
        glUnmapNamedBuffer(bufferName[Buffer.MODEL_MATRIX])

        glDeleteProgram(program.name)
        glDeleteVertexArrays(1, vertexArrayName)
        glDeleteBuffers(Buffer.MAX, bufferName)
        glDeleteTextures(1, textureName)
        glDeleteSamplers(1, samplerName)

        destroyBuffers(vertexArrayName, bufferName, textureName, samplerName, clearColor, clearDepth)
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            Thread { window.destroy() }.start()
        }
    }

    override fun keyReleased(e: KeyEvent) {}
}