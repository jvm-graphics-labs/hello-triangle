package gl3

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2ES2.GL_RED
import com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_TEXTURE_SWIZZLE_RGBA
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.texture.TextureIO
import framework.Semantic
import glm.L
import glm.SIZE
import glm.f
import glm.glm
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._4.Vec4
import uno.buffer.*
import uno.gl.checkError
import uno.glsl.Program
import kotlin.properties.Delegates

/**
 * Created by elect on 06/03/17.
 */

fun main(args: Array<String>) {
    HelloTextureK().setup()
}

class HelloTextureK : GLEventListener, KeyListener {

    var window by Delegates.notNull<GLWindow>()
    val animator = Animator()

    val vertexData = floatArrayOf(
            -1f, -1f, 0f, 0f,
            -1f, +1f, 0f, 1f,
            +1f, +1f, 1f, 1f,
            +1f, -1f, 1f, 0f)

    val elementData = shortArrayOf(0, 1, 3, 1, 2, 3)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val GLOBAL_MATRICES = 2
        val MAX = 3
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vertexArrayName = intBufferBig(1)

    val textureName = intBufferBig(1)
    val samplerName = intBufferBig(1)

    val clearColor = floatBufferBig(Vec4.length)
    val clearDepth = floatBufferBig(1)

    val matBuffer = floatBufferBig(16)

    var program by Delegates.notNull<Program>()

    var start = 0L

    fun setup() {

        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window.title = "Hello Texture"
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

        initTexture(gl)

        initSampler(gl)

        initProgram(gl)

        gl.glEnable(GL_DEPTH_TEST)

        start = System.currentTimeMillis()
    }

    fun initBuffers(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val elementBuffer = elementData.toShortBuffer()
        val globalMatricesBuffer = floatBufferBig(16 * 2)

        glGenBuffers(Buffer.MAX, bufferName)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.SIZE.L, elementBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)


        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        glBufferData(GL_UNIFORM_BUFFER, globalMatricesBuffer.SIZE.L, null, GL_STREAM_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, bufferName[Buffer.GLOBAL_MATRICES])


        vertexBuffer.destroy()
        elementBuffer.destroy()
        globalMatricesBuffer.destroy()

        checkError(gl, "initBuffers")
    }

    fun initVertexArray(gl: GL3) = with(gl) {

        glGenVertexArrays(1, vertexArrayName)
        glBindVertexArray(vertexArrayName[0])

        run {

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])

            run {
                val stride = Vec2.SIZE * 2
                var offset = 0

                glEnableVertexAttribArray(Semantic.Attr.POSITION)
                glVertexAttribPointer(Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, stride, offset.L)

                offset = Vec2.SIZE
                glEnableVertexAttribArray(Semantic.Attr.TEXCOORD)
                glVertexAttribPointer(Semantic.Attr.TEXCOORD, Vec2.length, GL_FLOAT, false, stride, offset.L)
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
        }
        glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    fun initTexture(gl: GL3) = with(gl) {

        val texture = this::class.java.classLoader.getResource("images/door.png")

        /* Texture data is an object containing all the relevant information about texture.    */
        val data = TextureIO.newTextureData(gl.glProfile, texture!!, false, TextureIO.PNG)

        // We don't use multiple levels (mipmaps) here, then our maximum level is zero.
        val level = 0

        glGenTextures(1, textureName)

        glBindTexture(GL_TEXTURE_2D, textureName[0])

        run {
            /* In this example internal format is GL_RGB8, dimensions are 512 x 512, border should always be zero,
            pixelFormat GL_RGB, pixelType GL_UNSIGNED_BYTE. */
            glTexImage2D(GL_TEXTURE_2D,
                    level,
                    data.internalFormat,
                    data.width, data.height,
                    data.border,
                    data.pixelFormat, data.pixelType,
                    data.buffer)

            // We set the base and max level.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, level)

            /* We set the swizzling. Since it is an RGB texture, we can choose to make the missing component alpha 
            equal to one.   */
            val swizzle = intBufferOf(GL_RED, GL_GREEN, GL_BLUE, GL_ONE)
            glTexParameterIiv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzle)

            swizzle.destroy()
        }
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun initSampler(gl: GL3) = with(gl) {

        /* As with most OpenGL objects, we create a sampler object with glGenSamplers. However, notice something unusual
        with the next series of functions. We do not bind a sampler to the context to set parameters in it, nor does
        glSamplerParameter take a context target. We simply pass an object directly to the function.         */
        glGenSamplers(1, samplerName)

        glSamplerParameteri(samplerName[0], GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glSamplerParameteri(samplerName[0], GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        /* OpenGL names the components of the texture coordinate “strq” rather than “xyzw” or “uvw” as is common.
        Indeed, OpenGL has two different names for the components: “strq” is used in the main API, but “stpq” is used in
        GLSL shaders. Much like “rgba”, you can use “stpq” as swizzle selectors for any vector instead of the
        traditional “xyzw”.
        The reason for the odd naming is that OpenGL tries to keep vector suffixes from conflicting. “uvw” does not work
        because “w” is already part of the “xyzw” suffix. In GLSL, the “r” in “strq” conflicts with “rgba”, so they had
        to go with “stpq” instead.  */
        glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glSamplerParameteri(samplerName[0], GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }


    fun initProgram(gl: GL3) = with(gl) {

        program = Program(gl, this::class.java, "shaders/gl3", "hello-texture.vert", "hello-texture.frag", "model", "diffuse")

        glUseProgram(program.name)
        /* We bind the uniform texture0UL to the Texture Image Units zero or, in other words, Semantic.Uniform.TEXTURE0.         */
        glUniform1i(program["diffuse"], Semantic.Sampler.DIFFUSE)
        glUseProgram(0)

        val globalMatricesBI = glGetUniformBlockIndex(program.name, "GlobalMatrices")

        if (globalMatricesBI == -1) {
            System.err.println("block index 'GlobalMatrices' not found!")
        }
        glUniformBlockBinding(program.name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES)

        checkError(gl, "initProgram")
    }

    override fun display(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        // update view matrix
        run {
            val view = Mat4()
            view to matBuffer

            glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
            glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, Mat4.SIZE.L, matBuffer)
            glBindBuffer(GL_UNIFORM_BUFFER, 0)
        }

        // We clear color and depth (although depth is not necessary since it is 1 by default).
        glClearBufferfv(GL_COLOR, 0, clearColor.put(0f, .33f, 0.66f, 1f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f))

        glUseProgram(program.name)
        glBindVertexArray(vertexArrayName[0])

        // update model matrix based on time
        run {
            val now = System.currentTimeMillis()
            val diff = (now - start).f / 1_000f

            // Here we build the matrix that will multiply our original vertex positions. We scale, half and rotate it.
            val model = Mat4()
                    .scale_(0.5f)
                    .rotate_(diff, 0f, 0f, 1f)

            glUniformMatrix4fv(program["model"], 1, false, model to matBuffer)
        }

        /* The glActiveTexture function changes the current texture unit. All subsequent texture operations, whether
            glBindTexture, glTexImage, glTexParameter, etc, affect the texture bound to the current texture unit.

            What this means is that if you want to modify a texture, you must overwrite a texture unit that may already 
            be bound. This is usually not a huge problem, because you rarely modify textures in the same area of code 
            used to render. But you should be aware of this.

            Also note the peculiar glActiveTexture syntax for specifying the image unit: GL_TEXTURE0 +
            Semantic.Uniform.TEXTURE0. This is the correct way to specify which texture unit, because glActiveTexture is
            defined in terms of an enumerator rather than integer texture image units.    */
        glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE)
        glBindTexture(GL_TEXTURE_2D, textureName[0])
        glBindSampler(Semantic.Sampler.DIFFUSE, samplerName[0])

        glDrawElements(GL_TRIANGLES, elementData.size, GL_UNSIGNED_SHORT, 0)

        glBindSampler(Semantic.Sampler.DIFFUSE, 0)
        glBindTexture(GL_TEXTURE_2D, 0)

        /* The following line binds VAO and program to the default values, this is not a free operation, it costs always
          (although very small). Every binding means additional validation and overhead, this may affect your
          performances in very specific scenario. So you should reduce these calls, but remember that OpenGL is a state
          machine, so what you left bound remains bound!    */
        glUseProgram(0)
        glBindVertexArray(0)

        /* Check always any GL error, but keep in mind this is an implicit synchronization between CPU and GPU, so you
          should use it only for debug purposes.    */
        checkError(gl, "display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = with(drawable.gl.gL3) {

        glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f) to matBuffer

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.GLOBAL_MATRICES])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE.L, matBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glViewport(x, y, width, height)

        checkError(gl, "reshape")
    }

    override fun dispose(drawable: GLAutoDrawable) = with(drawable.gl.gL3) {

        glDeleteProgram(program.name)
        glDeleteVertexArrays(1, vertexArrayName)
        glDeleteBuffers(Buffer.MAX, bufferName)
        glDeleteTextures(1, textureName)
        glDeleteSamplers(1, samplerName)

        destroyBuffers(vertexArrayName, bufferName, textureName, samplerName, matBuffer, clearColor, clearDepth)
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