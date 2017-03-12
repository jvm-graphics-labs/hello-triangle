
package gl4;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import framework.Semantic;
import glm.mat.Mat4x4;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import uno.debug.GlDebugOutput;
import uno.glsl.Program;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL3.*;
import static com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT;
import static com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;

/**
 * @author gbarbieri
 */
public class HelloTriangle implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new HelloTriangle().initGL();
    }

    private float[] vertexData = {
            -1, -1, 1, 0, 0,
            +0, +2, 0, 0, 1,
            +1, -1, 0, 1, 0};

    private short[] elementData = {0, 2, 1};

    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MODEL_MATRIX = 3;
        int MAX = 4;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    private ByteBuffer globalMatricesPointer, modelMatrixPointer;
    // https://jogamp.org/bugzilla/show_bug.cgi?id=1287
    private boolean bug1287 = true;

    private Program program;

    private long start;


    private void initGL() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Triangle (enhanced)");
        window.setSize(1024, 768);

        window.setVisible(true);

        window.addGLEventListener(this);
        window.addKeyListener(this);

        animator = new Animator(window);
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

        GL4 gl = drawable.getGL().getGL4();

        initDebug(gl);

        initBuffers(gl);

        initVertexArray(gl);

        program = new Program(gl, getClass(), "shaders/gl4", "hello-triangle.vert", "hello-triangle.frag");

        gl.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initDebug(GL4 gl) {

        window.getContext().addGLDebugListener(new GlDebugOutput());

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DONT_CARE,
                0,
                null,
                false);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_HIGH,
                0,
                null,
                true);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_MEDIUM,
                0,
                null,
                true);
    }

    private void initBuffers(GL4 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl.glNamedBufferStorage(bufferName.get(Buffer.VERTEX), vertexBuffer.capacity() * Float.BYTES, vertexBuffer,
                    GL_STATIC_DRAW);
            gl.glNamedBufferStorage(bufferName.get(Buffer.ELEMENT), elementBuffer.capacity() * Short.BYTES,
                    elementBuffer, GL_STATIC_DRAW);

            gl.glNamedBufferStorage(bufferName.get(Buffer.GLOBAL_MATRICES), Mat4x4.SIZE * 2, null, GL_MAP_WRITE_BIT);
            gl.glNamedBufferStorage(bufferName.get(Buffer.MODEL_MATRIX), Mat4x4.SIZE, null, GL_MAP_WRITE_BIT);

        } else {

            gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl.glBufferStorage(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, 0);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            gl.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, 0);
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);


            IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
            gl.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
            int globalBlockSize = Math.max(Mat4x4.SIZE * 2, uniformBufferOffset.get(0));
            int modelBlockSize = Math.max(Mat4x4.SIZE, uniformBufferOffset.get(0));

            gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
            gl.glBufferStorage(GL_UNIFORM_BUFFER, globalBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
            gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

            gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MODEL_MATRIX));
            gl.glBufferStorage(GL_UNIFORM_BUFFER, modelBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
            gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

            destroyBuffer(uniformBufferOffset);
        }

        destroyBuffers(vertexBuffer, elementBuffer);


        // map the transform buffers and keep them mapped
        globalMatricesPointer = gl.glMapNamedBufferRange(
                bufferName.get(Buffer.GLOBAL_MATRICES),
                0,
                Mat4x4.SIZE * 2,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT); // flags

        modelMatrixPointer = gl.glMapNamedBufferRange(
                bufferName.get(Buffer.MODEL_MATRIX),
                0,
                Mat4x4.SIZE,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    }

    private void initVertexArray(GL4 gl) {

        gl.glCreateVertexArrays(1, vertexArrayName);

        gl.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream.A);
        gl.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.COLOR, Semantic.Stream.A);

        gl.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, 0);
        gl.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.COLOR, Vec3.length, GL_FLOAT, false, Vec2.SIZE);

        gl.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);
        gl.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.COLOR);

        gl.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));
        gl.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream.A, bufferName.get(Buffer.VERTEX), 0,
                Vec2.SIZE + Vec3.SIZE);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();


        // view matrix
        {
            Mat4x4 view = new Mat4x4();
            view.to(globalMatricesPointer, Mat4x4.SIZE);
        }


        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1f).put(1, .5f).put(2, 0f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));

        // model matrix
        {
            long now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1_000;

            Mat4x4 model = new Mat4x4();
            model
                    .scale_(0.5f)
                    .rotate_(diff, 0f, 0f, 1f)
                    .to(modelMatrixPointer);
        }

        gl.glUseProgram(program.name);
        gl.glBindVertexArray(vertexArrayName.get(0));

        gl.glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM0,
                bufferName.get(Buffer.GLOBAL_MATRICES));

        gl.glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM1,
                bufferName.get(Buffer.MODEL_MATRIX));

        gl.glDrawElements(
                GL_TRIANGLES,
                elementData.length,
                GL_UNSIGNED_SHORT,
                0);

        gl.glUseProgram(0);
        gl.glBindVertexArray(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL4 gl = drawable.getGL().getGL4();

        // ortho matrix
        glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f).to(matBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4x4.SIZE, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        gl.glUnmapNamedBuffer(bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glUnmapNamedBuffer(bufferName.get(Buffer.MODEL_MATRIX));

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, vertexArrayName);
        gl.glDeleteBuffers(Buffer.MAX, bufferName);

        destroyBuffers(vertexArrayName, bufferName, clearColor, clearDepth);
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