/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helloTriangle;

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
import framework.BufferUtils;
import framework.Semantic;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author gbarbieri
 */
public class HelloTriangle implements GLEventListener, KeyListener {

    private static int screenIdx = 0;
    private static Dimension windowSize = new Dimension(1024, 768);
    private static boolean undecorated = false;
    private static boolean alwaysOnTop = false;
    private static boolean fullscreen = false;
    private static boolean mouseVisible = true;
    private static boolean mouseConfined = false;
    private static String title = "Hello Triangle";
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

        HelloTriangle helloTriangle = new HelloTriangle();
        glWindow.addGLEventListener(helloTriangle);
        glWindow.addKeyListener(helloTriangle);

        animator = new Animator(glWindow);
        animator.start();
    }

    private int[] objects = new int[Semantic.Object.SIZE];
    // Position interleaved with colors (to be normalized).
    private byte[] vertexData = new byte[]{
        (byte) -1, (byte) -1, Byte.MAX_VALUE, (byte) 0, (byte) 0,
        (byte) +0, (byte) +2, (byte) 0, (byte) 0, Byte.MAX_VALUE,
        (byte) +1, (byte) -1, (byte) 0, Byte.MAX_VALUE, (byte) 0
    };
    private short[] indexData = new short[]{
        0, 2, 1
    };
    private int program, modelToClipMatrixUL;
    private final String SHADERS_ROOT = "/shaders";
    /**
     * Use pools, you don't want to create and let them cleaned by the garbage
     * collector continuously in the display() method.
     */
    private float[] scale = new float[16];
    private float[] zRotazion = new float[16];
    private float[] modelToClip = new float[16];
    private long start, now;

    public HelloTriangle() {

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        initVbo(gl4);

        initIbo(gl4);

        initVao(gl4);

        initProgram(gl4);

        gl4.glEnable(GL4.GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initVbo(GL4 gl4) {

        gl4.glGenBuffers(1, objects, Semantic.Object.VBO);
        gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Semantic.Object.VBO]);
        {
            ByteBuffer vertexBuffer = GLBuffers.newDirectByteBuffer(vertexData);
            int size = vertexData.length * Byte.BYTES;
            gl4.glBufferData(GL4.GL_ARRAY_BUFFER, size, vertexBuffer, GL4.GL_STATIC_DRAW);
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
                 * VBO is not part of VAO, we need it to bind it only when we call
                 * glEnableVertexAttribArray and glVertexAttribPointer, so that VAO
                 * knows which VBO the attributes refer to, then we can unbind it.
                 */
                gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, objects[Semantic.Object.VBO]);
                {
                    /**
                     * This is the vertex attribute layout:
                     *
                     * | position x | position y | color R | color G | color B |
                     */
                    int stride = (2 + 3) * Byte.BYTES;
                    /**
                     * We draw in 2D on the xy plane, so we need just two
                     * coordinates for the position, it will be padded to vec4 as
                     * (x, y, 0, 1) in the vertex shader.
                     */
                    gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                    gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL4.GL_BYTE,
                            false, stride, 0 * Byte.BYTES);
                    /**
                     * Color needs three coordinates. We show the usage of normalization,
                     * where signed value get normalized [-1, 1] like in this case.
                     * unsigned will get normalized in the [0, 1] instead, but take
                     * in account java use always signed, althought you can trick it.
                     * Vec3 color will be padded to (x, y, z, 1) in the fragment
                     * shader.
                     */
                    gl4.glEnableVertexAttribArray(Semantic.Attr.COLOR);
                    gl4.glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL4.GL_BYTE,
                            true, stride, 2 * Byte.BYTES);
                }
                gl4.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            }
        }
        gl4.glBindVertexArray(0);

        checkError(gl4, "initVao");
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
         * These links don't go into effect until you link the program. If you want
         * to change index, you need to link the program again.
         */
        gl4.glBindAttribLocation(program, Semantic.Attr.POSITION, "position");
        gl4.glBindAttribLocation(program, Semantic.Attr.COLOR, "color");
        gl4.glBindFragDataLocation(program, Semantic.Frag.COLOR, "outputColor");

        shaderProgram.link(gl4, System.out);
        /**
         * Take in account that JOGL offers a GLUniformData class, here we don't
         * use it, but take a look to it since it may be interesting for you.
         */
        modelToClipMatrixUL = gl4.glGetUniformLocation(program, "modelToClipMatrix");

        checkError(gl4, "initProgram");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glDeleteProgram(program);
        /**
         * Clean VAO first in order to minimize problems. If you delete IBO first,
         * VAO will still have the IBO id, this may lead to crashes.
         */
        gl4.glDeleteVertexArrays(1, objects, objects[Semantic.Object.VAO]);

        gl4.glDeleteBuffers(1, objects, Semantic.Object.VBO);

        gl4.glDeleteBuffers(1, objects, Semantic.Object.IBO);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL4 gl4 = drawable.getGL().getGL4();

        /**
         * We set the clear color and depth (althought depth is not necessary since
         * it is 1 by default).
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
                scale = FloatUtil.makeScale(scale, true, 0.5f, 0.5f, 0.5f);
                zRotazion = FloatUtil.makeRotationEuler(zRotazion, 0, 0, 0, diff);
                modelToClip = FloatUtil.multMatrix(scale, zRotazion);
                gl4.glUniformMatrix4fv(modelToClipMatrixUL, 1, false, modelToClip, 0);

                gl4.glDrawElements(GL4.GL_TRIANGLES, indexData.length, GL4.GL_UNSIGNED_SHORT, 0);
            }
            /**
             * In this sample we bind VAO to the default values, this is not a
             * cheapier binding, it costs always as a binding, so here we have for
             * example 2 vao bindings. Every binding means additional validation
             * and overhead, this may affect your performances.
             * So if you are looking for high performances skip these calls, but
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
            HelloTriangle.animator.stop();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
