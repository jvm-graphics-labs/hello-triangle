/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gl3.helloTriangle;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
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
import framework.Semantic;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author gbarbieri
 */
public class HelloTriangle implements GLEventListener, KeyListener {

    public static GLWindow glWindow;
    public static Animator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(1024, 768);
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle("Hello Triangle");

        glWindow.setVisible(true);

        HelloTriangle helloTriangle = new HelloTriangle();
        glWindow.addGLEventListener(helloTriangle);
        glWindow.addKeyListener(helloTriangle);

        animator = new Animator(glWindow);
        animator.start();
    }

    private final String SHADERS_ROOT = "src/gl3/helloTriangle/shaders";
    private final String SHADERS_NAME = "hello-triangle";

    private int vertexCount = 3;
    private int vertexSize = vertexCount * 5 * Float.BYTES;
    private float[] vertexData = new float[]{
        -1, -1,/**/ 1, 0, 0,
        +0, +2,/**/ 0, 0, 1,
        +1, -1,/**/ 0, 1, 0
    };

    private int elementCount = 3;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = new short[]{
        0, 2, 1
    };

    private static class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private int programName, modelToClipMatrixUL;
    /**
     * Use pools, you don't want to create and let them cleaned by the garbage
     * collector continuously in the display() method.
     */
    private float[] scale = new float[16], zRotazion = new float[16], modelToClip = new float[16];
    private long start, now;

    public HelloTriangle() {
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL3 gl3 = drawable.getGL().getGL3();

        initBuffers(gl3);

        initVertexArray(gl3);

        initProgram(gl3);

        gl3.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initBuffers(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl3.glGenBuffers(Buffer.MAX, bufferName);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        {
            gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        {
            gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(elementBuffer);

        checkError(gl3, "initBuffers");
    }

    private void initVertexArray(GL3 gl3) {
        /**
         * Let's create the VAO and save in it all the attributes properties.
         */
        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            /**
             * VBO is not part of VAO, we need it to bind it only when we call
             * glEnableVertexAttribArray and glVertexAttribPointer, so that VAO
             * knows which VBO the attributes refer to, then we can unbind it.
             */
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                /**
                 * This is the vertex attribute layout:
                 *
                 * | position x | position y | color R | color G | color B |
                 */
                int stride = (2 + 3) * Float.BYTES;
                int offset = 0 * Float.BYTES;
                /**
                 * We draw in 2D on the xy plane, so we need just two
                 * coordinates for the position, it will be padded to vec4 as
                 * (x, y, 0, 1) in the vertex shader.
                 */
                gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, stride, offset);
                /**
                 * Color needs three coordinates. Vec3 color will be padded to
                 * (x, y, z, 1) in the fragment shader.
                 */
                offset = 2 * Float.BYTES;
                gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
                gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL_FLOAT, false, stride, offset);
            }
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            /**
             * Ibo is part of the VAO, so we need to bind it and leave it bound.
             */
            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl3.glBindVertexArray(0);

        checkError(gl3, "initVao");
    }

    private void initProgram(GL3 gl3) {

        ShaderCode vertShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT,
                null, SHADERS_NAME, "vert", null, true);
        ShaderCode fragShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT,
                null, SHADERS_NAME, "frag", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl3);

        programName = shaderProgram.program();

        /**
         * These links don't go into effect until you link the program. If you
         * want to change index, you need to link the program again.
         */
        gl3.glBindAttribLocation(programName, Semantic.Attr.POSITION, "position");
        gl3.glBindAttribLocation(programName, Semantic.Attr.COLOR, "color");
        gl3.glBindFragDataLocation(programName, Semantic.Frag.COLOR, "outputColor");

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
        gl3.glDeleteVertexArrays(1, vertexArrayName);
        gl3.glDeleteBuffers(Buffer.MAX, bufferName);

        System.exit(0);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL3 gl3 = drawable.getGL().getGL3();

        /**
         * We set the clear color and depth (although depth is not necessary
         * since it is 1 by default).
         */
        gl3.glClearColor(0f, .33f, 0.66f, 1f);
        gl3.glClearDepthf(1f);
        gl3.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        {
            // update matrix based on time
            now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1000;
            /**
             * Here we build the matrix that will multiply our original vertex
             * positions. We scale, halving it, and rotate it.
             */
            scale = FloatUtil.makeScale(scale, true, 0.5f, 0.5f, 0.5f);
            zRotazion = FloatUtil.makeRotationEuler(zRotazion, 0, 0, 0, diff);
            modelToClip = FloatUtil.multMatrix(scale, zRotazion);
        }
        gl3.glUseProgram(programName);        
        gl3.glBindVertexArray(vertexArrayName.get(0));
        
        gl3.glUniformMatrix4fv(modelToClipMatrixUL, 1, false, modelToClip, 0);

        gl3.glDrawElements(GL_TRIANGLES, elementSize, GL_UNSIGNED_SHORT, 0);
        /**
         * The following line binds VAO and program to the default values, this
         * is not a cheaper binding, it costs always as a binding. Every binding
         * means additional validation and overhead, this may affect your
         * performances. So you should avoid these calls, but remember that
         * OpenGL is a state machine, so what you left bound remains bound!
         */
//        gl3.glBindVertexArray(0);
//        gl3.glUseProgram(0);
        /**
         * Check always any GL error, but keep in mind this is an implicit
         * synchronization between CPU and GPU, so you should use it only for
         * debug purposes.
         */
        checkError(gl3, "display");
    }

    protected void checkError(GL gl, String location) {

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
            System.out.println("OpenGL Error(" + errorString + "): " + location);
            throw new Error();
        }
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
            animator.remove(glWindow);
            glWindow.destroy();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
