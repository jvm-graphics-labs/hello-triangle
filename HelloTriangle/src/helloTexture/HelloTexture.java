/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helloTexture;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_INVALID_ENUM;
import static com.jogamp.opengl.GL.GL_INVALID_FRAMEBUFFER_OPERATION;
import static com.jogamp.opengl.GL.GL_INVALID_OPERATION;
import static com.jogamp.opengl.GL.GL_INVALID_VALUE;
import static com.jogamp.opengl.GL.GL_NO_ERROR;
import static com.jogamp.opengl.GL.GL_OUT_OF_MEMORY;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import framework.BufferUtils;
import framework.Semantic;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gbarbieri
 */
public class HelloTexture implements GLEventListener, KeyListener {

    private static int screenIdx = 0;
    private static Dimension windowSize = new Dimension(1024, 768);
    private static boolean undecorated = false;
    private static boolean alwaysOnTop = false;
    private static boolean fullscreen = false;
    private static boolean mouseVisible = true;
    private static boolean mouseConfined = false;
    private static String title = "Hello Texture";
    public static GLWindow glWindow;
    public static Animator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, screenIdx);
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(windowSize.getWidth(), windowSize.getHeight());
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);
        glWindow.setTitle(title);
        glWindow.setVisible(true);

        HelloTexture helloTexture = new HelloTexture();
        glWindow.addGLEventListener(helloTexture);
        glWindow.addKeyListener(helloTexture);

        animator = new Animator(glWindow);
        animator.start();
    }

    private int[] objects = new int[Semantic.Object.SIZE];
    // Position interleaved with texture coordinate.
    private float[] vertexData = new float[]{
        -0.5f, -0.5f, 0f, 0f,
        -0.5f, +0.5f, 0f, 1f,
        +0.5f, +0.5f, 1f, 1f,
        +0.5f, -0.5f, 1f, 0f
    };
    private short[] indexData = new short[]{
        0, 1, 3,
        1, 2, 3
    };
    private int program, modelToClipMatrixUL, texture0UL;
    private final String SHADERS_ROOT = "src/helloTexture/shaders";
    private final String TEXTURE_ROOT = "src/helloTexture/asset";
    private final String TEXTURE_NAME = "texture.png";
    /**
     * Use pools, you don't want to create and let them cleaned by the garbage
     * collector continuosly in the display() method.
     */
    private float[] zRotazion = new float[16];
    private long start, now;

    public HelloTexture() {

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        initVbo(gl4);

        initIbo(gl4);

        initVao(gl4);

        initTexture(gl4);

        initSampler(gl4);

        initProgram(gl4);

        gl4.glEnable(GL4.GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initVbo(GL4 gl4) {

        gl4.glGenBuffers(1, objects, Semantic.Object.VBO);
        gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Semantic.Object.VBO]);
        {
            FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
            int size = vertexData.length * Float.BYTES;
            gl4.glBufferData(GL4.GL_ARRAY_BUFFER, size, vertexBuffer, GL4.GL_STATIC_DRAW);
            /**
             * Since vertexBuffer is a direct buffer, this means it is outside
             * the Garbage Collector job and it is up to us to remove it.
             */
            BufferUtils.destroyDirectBuffer(vertexBuffer);
        }
        gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        checkError(gl4, "initVbo");
    }

    private void initIbo(GL4 gl4) {

        gl4.glGenBuffers(1, objects, Semantic.Object.IBO);
        gl4.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, objects[Semantic.Object.IBO]);
        {
            ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);
            int size = indexData.length * Short.BYTES;
            gl4.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, size, indexBuffer, GL4.GL_STATIC_DRAW);
            BufferUtils.destroyDirectBuffer(indexBuffer);
        }
        gl4.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, 0);

        checkError(gl4, "initIbo");
    }

    private void initVao(GL4 gl4) {
        /**
         * Let's create the VAO and save in it all the attributes properties.
         */
        gl4.glGenVertexArrays(1, objects, Semantic.Object.VAO);
        gl4.glBindVertexArray(objects[Semantic.Object.VAO]);
        {
            /**
             * Ibo is part of the VAO, so we need to bind it and leave it bound.
             */
            gl4.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, objects[Semantic.Object.IBO]);
            {
                /**
                 * VBO is not part of VAO, we need it to bind it only when we
                 * call glEnableVertexAttribArray and glVertexAttribPointer, so
                 * that VAO knows which VBO the attributes refer to, then we can
                 * unbind it.
                 */
                gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Semantic.Object.VBO]);
                {
                    int stride = (2 + 2) * Float.BYTES;
                    /**
                     * We draw in 2D on the xy plane, so we need just two
                     * coordinates for the position, it will be padded to vec4
                     * as (x, y, 0, 1) in the vertex shader.
                     */
                    gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                    gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL4.GL_FLOAT,
                            false, stride, 0 * Float.BYTES);
                    /**
                     * 2D Texture coordinates.
                     */
                    gl4.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
                    gl4.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL4.GL_FLOAT,
                            false, stride, 2 * Float.BYTES);
                }
                gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            }
        }
        gl4.glBindVertexArray(0);

        checkError(gl4, "initVao");
    }

    private void initTexture(GL4 gl4) {

        try {
            File textureFile = new File(TEXTURE_ROOT + "/" + TEXTURE_NAME);

            /**
             * Texture data is an object containing all the relevant information
             * about texture.
             */
            TextureData textureData = TextureIO.newTextureData(gl4.getGLProfile(), textureFile, false, TextureIO.PNG);
            /**
             * We don't use multiple levels (mipmaps) here, then our maximum
             * level is zero.
             */
            int level = 0;

            gl4.glGenTextures(1, objects, Semantic.Object.TEXTURE);

            gl4.glBindTexture(GL4.GL_TEXTURE_2D, objects[Semantic.Object.TEXTURE]);
            {
                /**
                 * In this example internal format is GL_RGB8, dimensions are
                 * 512 x 512, border should always be zero, pixelFormat GL_RGB,
                 * pixelType GL_UNSIGNED_BYTE.
                 */
                gl4.glTexImage2D(GL4.GL_TEXTURE_2D, level, textureData.getInternalFormat(),
                        textureData.getWidth(), textureData.getHeight(), textureData.getBorder(),
                        textureData.getPixelFormat(), textureData.getPixelType(), textureData.getBuffer());
                /**
                 * We set the base and max level.
                 */
                gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_BASE_LEVEL, 0);
                gl4.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAX_LEVEL, level);
                /**
                 * We set the swizzling. Since it is an RGB texture, we can
                 * choose to make the missing component alpha equal to one.
                 */
                int[] swizzle = new int[]{GL4.GL_RED, GL4.GL_GREEN, GL4.GL_BLUE, GL4.GL_ONE};
                gl4.glTexParameterIiv(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_SWIZZLE_RGBA, swizzle, 0);
            }
            gl4.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        } catch (IOException ex) {
            Logger.getLogger(HelloTexture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initSampler(GL4 gl4) {

        /**
         * As with most OpenGL objects, we create a sampler object with
         * glGenSamplers. However, notice something unusual with the next series
         * of functions. We do not bind a sampler to the context to set
         * parameters in it, nor does glSamplerParameter take a context target.
         * We simply pass an object directly to the function.
         */
        gl4.glGenSamplers(1, objects, Semantic.Object.SAMPLER);

        gl4.glSamplerParameteri(objects[Semantic.Object.SAMPLER], GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl4.glSamplerParameteri(objects[Semantic.Object.SAMPLER], GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        /**
         * OpenGL names the components of the texture coordinate “strq” rather
         * than “xyzw” or “uvw” as is common. Indeed, OpenGL has two different
         * names for the components: “strq” is used in the main API, but “stpq”
         * is used in GLSL shaders. Much like “rgba”, you can use “stpq” as
         * swizzle selectors for any vector instead of the traditional “xyzw”.
         * The reason for the odd naming is that OpenGL tries to keep vector
         * suffixes from conflicting. “uvw” does not work because “w” is already
         * part of the “xyzw” suffix. In GLSL, the “r” in “strq” conflicts with
         * “rgba”, so they had to go with “stpq” instead.
         */
        gl4.glSamplerParameteri(objects[Semantic.Object.SAMPLER], GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(objects[Semantic.Object.SAMPLER], GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
    }

    private void initProgram(GL4 gl4) {
        ShaderCode vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(),
                SHADERS_ROOT, null, "vs", "glsl", null, true);
        ShaderCode fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(),
                SHADERS_ROOT, null, "fs", "glsl", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl4);

        program = shaderProgram.program();

        /**
         * These links don't go into effect until you link the program. If you
         * want to change index, you need to link the program again.
         */
        gl4.glBindAttribLocation(program, Semantic.Attr.POSITION, "position");
        gl4.glBindAttribLocation(program, Semantic.Attr.TEXCOORD, "texCoord");
        gl4.glBindFragDataLocation(program, Semantic.Frag.COLOR, "outputColor");

        shaderProgram.link(gl4, System.out);
        /**
         * Take in account that JOGL offers a GLUniformData class, here we don't
         * use it, but take a look to it since it may be interesting for you.
         */
        modelToClipMatrixUL = gl4.glGetUniformLocation(program, "modelToClipMatrix");
        texture0UL = gl4.glGetUniformLocation(program, "texture0");

        gl4.glUseProgram(program);
        {
            /**
             * We bind the uniform texture0UL to the Texture Image Units zero
             * or, in other words, Semantic.Uniform.TEXTURE0.
             */
            gl4.glUniform1i(texture0UL, Semantic.Sampler.DIFFUSE);
        }
        gl4.glUseProgram(0);

        checkError(gl4, "initProgram");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glDeleteProgram(program);
        /**
         * Clean VAO first in order to minimize problems. If you delete IBO
         * first, VAO will still have the IBO id, this may lead to crashes.
         */
        gl4.glDeleteVertexArrays(1, objects, objects[Semantic.Object.VAO]);

        gl4.glDeleteBuffers(1, objects, Semantic.Object.VBO);

        gl4.glDeleteBuffers(1, objects, Semantic.Object.IBO);

        gl4.glDeleteTextures(1, objects, Semantic.Object.TEXTURE);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL4 gl4 = drawable.getGL().getGL4();

        /**
         * We set the clear color and depth (althought depth is not necessary
         * since it is 1 by default).
         */
        gl4.glClearColor(0f, .33f, 0.66f, 1f);
        gl4.glClearDepthf(1f);
        gl4.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        gl4.glUseProgram(program);
        {
            gl4.glBindVertexArray(objects[Semantic.Object.VAO]);
            {
                now = System.currentTimeMillis();
                float diff = (float) (now - start) / 1000;
                /**
                 * Here we build the matrix that will multiply our original
                 * vertex positions. We scale, halving it, and rotate it.
                 */
                zRotazion = FloatUtil.makeRotationEuler(zRotazion, 0, 0, 0, diff);
                gl4.glUniformMatrix4fv(modelToClipMatrixUL, 1, false, zRotazion, 0);
                /**
                 * The glActiveTexture function changes the current texture
                 * unit. All subsequent texture operations, whether
                 * glBindTexture, glTexImage, glTexParameter, etc, affect the
                 * texture bound to the current texture unit.
                 *
                 * What this means is that if you want to modify a texture, you
                 * must overwrite a texture unit that may already be bound. This
                 * is usually not a huge problem, because you rarely modify
                 * textures in the same area of code used to render. But you
                 * should be aware of this.
                 *
                 * Also note the peculiar glActiveTexture syntax for specifying
                 * the image unit: GL_TEXTURE0 + Semantic.Uniform.TEXTURE0. This
                 * is the correct way to specify which texture unit, because
                 * glActiveTexture is defined in terms of an enumerator rather
                 * than integer texture image units.
                 */
                gl4.glActiveTexture(GL4.GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
                gl4.glBindTexture(GL4.GL_TEXTURE_2D, objects[Semantic.Object.TEXTURE]);
                {
                    gl4.glBindSampler(Semantic.Sampler.DIFFUSE, objects[Semantic.Object.SAMPLER]);
                    {
                        gl4.glDrawElements(GL4.GL_TRIANGLES, indexData.length, GL4.GL_UNSIGNED_SHORT, 0);
                    }
                    gl4.glBindSampler(Semantic.Sampler.DIFFUSE, 0);
                }
                gl4.glBindTexture(GL4.GL_TEXTURE_2D, 0);
            }
            /**
             * In this sample we bind VAO to the default values, this is not a
             * cheapier binding, it costs always as a binding, so here we have
             * for example 2 vao bindings. Every binding means additional
             * validation and overhead, this may affect your performances. So if
             * you are looking for high performances skip these calls, but
             * remember that OpenGL is a state machine, so what you left bound
             * remains bound!
             */
            gl4.glBindVertexArray(0);
        }
        gl4.glUseProgram(0);
        /**
         * Check always any GL error, but keep in mind this is an implicit
         * synchronization between CPU and GPU, so you should use it only for
         * debug purposes.
         */
        checkError(gl4, "display");
    }

    protected boolean checkError(GL gl, String title) {

        int error = gl.glGetError();
        if (error != GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                case GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "UNKNOWN";
                    break;
            }
            System.out.println("OpenGL Error(" + errorString + "): " + title);
            throw new Error();
        }
        return error == GL_NO_ERROR;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.out.println("reshape");
        GL4 gl4 = drawable.getGL().getGL4();
        /**
         * Just the glViewport for this sample, normally here you update your
         * projection matrix.
         */
        gl4.glViewport(x, y, width, height);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            HelloTexture.animator.stop();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
