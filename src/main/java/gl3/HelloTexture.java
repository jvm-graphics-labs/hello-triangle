
package gl3;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import framework.Semantic;
import glm.mat.Mat4x4;
import glm.vec._2.Vec2;
import uno.glsl.Program;

import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2ES3.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL2ES3.GL_NEAREST;
import static com.jogamp.opengl.GL2ES3.GL_ONE;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE0;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_SWIZZLE_RGBA;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.gl.GlErrorKt.checkError;

/**
 * @author gbarbieri
 */
public class HelloTexture implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new HelloTextureK().setup();
    }

    private float[] vertexData = {
            -1f, -1f, 0f, 0f,
            -1f, +1f, 0f, 1f,
            +1f, +1f, 1f, 1f,
            +1f, -1f, 1f, 0f};

    private short[] elementData = {
            0, 1, 3,
            1, 2, 3};

    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(1);
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    private Program program;

    private long start;

    private void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Texture");
        window.setSize(1024, 768);

        window.setVisible(true);

        window.addGLEventListener(this);
        window.addKeyListener(this);

        animator = new Animator();
        animator.add(window);
        animator.start();

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
                System.exit(1);
            }
        });

    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        initBuffers(gl);

        initVertexArray(gl);

        initTexture(gl);

        initSampler(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initBuffers(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);


        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4x4.SIZE * 2, null, GL_STREAM_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, bufferName.get(Buffer.GLOBAL_MATRICES));


        destroyBuffers(vertexBuffer, elementBuffer);

        checkError(gl, "initBuffers");
    }

    private void initVertexArray(GL3 gl) {

        gl.glGenVertexArrays(1, vertexArrayName);
        gl.glBindVertexArray(vertexArrayName.get(0));
        {
            gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                int stride = Vec2.SIZE * 2;
                int offset = 0;

                gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                gl.glVertexAttribPointer(Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, stride, offset);

                offset = Vec2.SIZE;
                gl.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
                gl.glVertexAttribPointer(Semantic.Attr.TEXCOORD, Vec2.length, GL_FLOAT, false, stride, offset);
            }
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl.glBindVertexArray(0);

        checkError(gl, "initVao");
    }

    private void initTexture(GL3 gl) {

        try {
            URL texture = getClass().getClassLoader().getResource("images/door.png");

            /* Texture data is an object containing all the relevant information about texture.    */
            TextureData data = TextureIO.newTextureData(gl.getGLProfile(), texture, false, TextureIO.PNG);

            int level = 0;

            gl.glGenTextures(1, textureName);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
            {
                gl.glTexImage2D(GL_TEXTURE_2D,
                        level,
                        data.getInternalFormat(),
                        data.getWidth(), data.getHeight(),
                        data.getBorder(),
                        data.getPixelFormat(), data.getPixelType(),
                        data.getBuffer());

                gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
                gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, level);

                IntBuffer swizzle = GLBuffers.newDirectIntBuffer(new int[]{GL_RED, GL_GREEN, GL_BLUE, GL_ONE});
                gl.glTexParameterIiv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzle);

                destroyBuffer(swizzle);
            }
            gl.glBindTexture(GL_TEXTURE_2D, 0);

        } catch (IOException ex) {
            Logger.getLogger(HelloTextureK.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initSampler(GL3 gl) {

        gl.glGenSamplers(1, samplerName);

        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }


    private void initProgram(GL3 gl) {

        program = new Program(gl, getClass(), "shaders/gl3", "hello-texture.vert", "hello-texture.frag", "modelToWorldMat", "diffuse");

        gl.glUseProgram(program.name);

        gl.glUniform1i(program.get("diffuse"), Semantic.Sampler.DIFFUSE);
        gl.glUseProgram(0);

        int globalMatricesBI = gl.glGetUniformBlockIndex(program.name, "GlobalMatrices");

        if (globalMatricesBI == -1) {
            System.err.println("block index 'GlobalMatrices' not found!");
        }
        gl.glUniformBlockBinding(program.name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES);


        checkError(gl, "initProgram");
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        // view matrix
        {
            Mat4x4 view = new Mat4x4();
            view.to(matBuffer);

            gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
            gl.glBufferSubData(GL_UNIFORM_BUFFER, Mat4x4.SIZE, Mat4x4.SIZE, matBuffer);
            gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));

        gl.glUseProgram(program.name);
        gl.glBindVertexArray(vertexArrayName.get(0));

        // model matrix
        {
            long now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1_000f;

            Mat4x4 model = new Mat4x4();
            model
                    .scale(0.5f)
                    .rotate(diff, 0f, 0f, 1f)
                    .to(matBuffer);

            gl.glUniformMatrix4fv(program.get("modelToWorldMat"), 1, false, matBuffer);
        }

        gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(0));
        {
            gl.glDrawElements(GL_TRIANGLES, elementData.length, GL_UNSIGNED_SHORT, 0);
        }
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, 0);
        gl.glBindTexture(GL_TEXTURE_2D, 0);

        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        checkError(gl, "display");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3 gl = drawable.getGL().getGL3();

        // ortho matrix
        glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f).to(matBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4x4.SIZE, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(x, y, width, height);

        checkError(gl, "reshape");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, vertexArrayName);
        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteTextures(1, textureName);
        gl.glDeleteSamplers(1, samplerName);

        destroyBuffers(vertexArrayName, bufferName, textureName, samplerName, matBuffer, clearColor, clearDepth);
    }


    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            new Thread(() -> {
                window.destroy();
            }).start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}