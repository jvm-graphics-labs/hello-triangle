/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gl3.helloTriangle;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3.*;
import com.jogamp.opengl.GL3;
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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
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

    private static class Object {

        public static final int VBO = 0;
        public static final int IBO = 1;
        public static final int VAO = 2;
        public static final int SIZE = 3;
    }

    private static class Attribute {

        public static final int POSITION = 0;
        public static final int COLOR = 1;
        public static final int SIZE = 2;
    }

    private static class Fragment {

        public static final int COLOR = 0;
        public static final int SIZE = 1;
    }

    private IntBuffer objectsName = GLBuffers.newDirectIntBuffer(Object.SIZE);

    private byte[] vertexData = new byte[]{
        (byte) -1, (byte) -1, Byte.MAX_VALUE, (byte) 0, (byte) 0,
        (byte) +0, (byte) +2, (byte) 0, (byte) 0, Byte.MAX_VALUE,
        (byte) +1, (byte) -1, (byte) 0, Byte.MAX_VALUE, (byte) 0
    };

    private short[] indexData = new short[]{
        0, 2, 1
    };

    private int programName, modelToClipMatrixUL;
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

        GL3 gl3 = drawable.getGL().getGL3();

        initVbo(gl3);

        initIbo(gl3);

        initVao(gl3);

        initProgram(gl3);

        gl3.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initVbo(GL3 gl3) {

        objectsName.position(Object.VBO);
        gl3.glGenBuffers(1, objectsName);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, objectsName.get(Object.VBO));
        {
            ByteBuffer vertexBuffer = GLBuffers.newDirectByteBuffer(vertexData);
            int size = vertexData.length * Byte.BYTES;
            gl3.glBufferData(GL_ARRAY_BUFFER, size, vertexBuffer, GL_STATIC_DRAW);
            BufferUtils.destroyDirectBuffer(vertexBuffer);
        }
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        checkError(gl3, "initVbo");
    }

    private void initIbo(GL3 gl3) {

        objectsName.position(Object.IBO);
        gl3.glGenBuffers(1, objectsName);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, objectsName.get(Object.IBO));
        {
            ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);
            int size = indexData.length * Short.BYTES;
            gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, size, indexBuffer, GL_STATIC_DRAW);
            BufferUtils.destroyDirectBuffer(indexBuffer);
        }
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        checkError(gl3, "initIbo");
    }

    private void initVao(GL3 gl3) {
        /**
         * Let's create the VAO and save in it all the attributes properties.
         */
        objectsName.position(Object.VAO);
        gl3.glGenVertexArrays(1, objectsName);
        gl3.glBindVertexArray(objectsName.get(Object.VAO));
        {
            /**
             * Ibo is part of the VAO, so we need to bind it and leave it bound.
             */
            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, objectsName.get(Object.IBO));
            {
                /**
                 * VBO is not part of VAO, we need it to bind it only when we
                 * call glEnableVertexAttribArray and glVertexAttribPointer, so
                 * that VAO knows which VBO the attributes refer to, then we can
                 * unbind it.
                 */
                gl3.glBindBuffer(GL_ARRAY_BUFFER, objectsName.get(Object.VBO));
                {
                    /**
                     * This is the vertex attribute layout:
                     *
                     * | position x | position y | color R | color G | color B |
                     */
                    int stride = (2 + 3) * Byte.BYTES;
                    int offset = 0 * Byte.BYTES;
                    /**
                     * We draw in 2D on the xy plane, so we need just two
                     * coordinates for the position, it will be padded to vec4
                     * as (x, y, 0, 1) in the vertex shader.
                     */
                    gl3.glEnableVertexAttribArray(Attribute.POSITION);
                    gl3.glVertexAttribPointer(Attribute.POSITION, 2, GL_BYTE, false, stride, offset);
                    /**
                     * Color needs three coordinates. We show the usage of
                     * normalization, where signed value get normalized [-1, 1]
                     * like in this case. unsigned will get normalized in the
                     * [0, 1] instead, but take in account java use always
                     * signed, althought you can trick it. Vec3 color will be
                     * padded to (x, y, z, 1) in the fragment shader.
                     */
                    offset = 2 * Byte.BYTES;
                    gl3.glEnableVertexAttribArray(Attribute.COLOR);
                    gl3.glVertexAttribPointer(Attribute.COLOR, 3, GL_BYTE, true, stride, offset);
                }
                gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);
            }
        }
        gl3.glBindVertexArray(0);

        checkError(gl3, "initVao");
    }

    private void initProgram(GL3 gl3) {

        ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT,
                null, "vs", "glsl", null, true);
        ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, 
                null, "fs", "glsl", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl3);

        programName = shaderProgram.program();

        /**
         * These links don't go into effect until you link the program. If you
         * want to change index, you need to link the program again.
         */
        gl3.glBindAttribLocation(programName, Attribute.POSITION, "position");
        gl3.glBindAttribLocation(programName, Attribute.COLOR, "color");
        gl3.glBindFragDataLocation(programName, Fragment.COLOR, "outputColor");

        shaderProgram.link(gl3, System.out);
        /**
         * Take in account that JOGL offers a GLUniformData class, here we don't
         * use it, but take a look to it since it may be interesting for you.
         */
        modelToClipMatrixUL = gl3.glGetUniformLocation(programName, "modelToClipMatrix");

        checkError(gl3, "initProgram");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");

        GL3 gl3 = drawable.getGL().getGL3();

        gl3.glDeleteProgram(programName);
        /**
         * Clean VAO first in order to minimize problems. If you delete IBO
         * first, VAO will still have the IBO id, this may lead to crashes.
         */
        objectsName.position(Object.VAO);
        gl3.glDeleteVertexArrays(1, objectsName);
        objectsName.position(Object.VBO);
        gl3.glDeleteBuffers(1, objectsName);
        objectsName.position(Object.IBO);
        gl3.glDeleteBuffers(1, objectsName);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL3 gl3 = drawable.getGL().getGL3();

        /**
         * We set the clear color and depth (althought depth is not necessary
         * since it is 1 by default).
         */
        gl3.glClearColor(0f, .33f, 0.66f, 1f);
        gl3.glClearDepthf(1f);
        gl3.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        gl3.glUseProgram(programName);
        {
            gl3.glBindVertexArray(objectsName.get(Object.VAO));
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
                gl3.glUniformMatrix4fv(modelToClipMatrixUL, 1, false, modelToClip, 0);

                gl3.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
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
            gl3.glBindVertexArray(0);
        }
        gl3.glUseProgram(0);
        /**
         * Check always any GL error, but keep in mind this is an implicit
         * synchronization between CPU and GPU, so you should use it only for
         * debug purposes.
         */
        checkError(gl3, "display");
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
        GL3 gl3 = drawable.getGL().getGL3();
        /**
         * Just the glViewport for this sample, normally here you update your
         * projection matrix.
         */
        gl3.glViewport(x, y, width, height);
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
