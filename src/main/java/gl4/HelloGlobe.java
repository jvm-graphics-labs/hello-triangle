
package gl4;

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
import glm.vec._3.Vec3;
import uno.debug.GlDebugOutput;
import uno.glsl.Program;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_MAP_INVALIDATE_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_MAP_WRITE_BIT;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import static com.jogamp.opengl.GL4.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL4.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL4.*;
import static com.jogamp.opengl.GL4.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL4.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL4.GL_NEAREST;
import static com.jogamp.opengl.GL4.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL4.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL4.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL4.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL4.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL4.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL4.GL_TRIANGLES;
import static com.jogamp.opengl.GL4.GL_UNSIGNED_SHORT;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;

/**
 * @author elect
 */
public class HelloGlobe implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new HelloGlobe().setup();
    }

    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MODEL_MATRIX = 3;
        int MAX = 4;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(1);
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private ByteBuffer globalMatricesPointer, modelMatrixPointer;
    // https://jogamp.org/bugzilla/show_bug.cgi?id=1287
    private boolean bug1287 = true;

    private Program program;

    private long start;

    private int elementCount;

    private void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Globe");
        window.setSize(1024, 768);

        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
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

        initTexture(gl);

        initSampler(gl);

        initVertexArray(gl);

        program = new Program(gl, getClass(), "shaders/gl4", "hello-globe.vert", "hello-globe.frag");

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

        float radius = 1f;
        short rings = 100;
        short sectors = 100;

        FloatBuffer vertexBuffer = getVertexBuffer(radius, rings, sectors);
        ShortBuffer elementBuffer = getElementBuffer(radius, rings, sectors);

        elementCount = elementBuffer.capacity();

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
            int globalBlockSize = glm.max(Mat4x4.SIZE * 2, uniformBufferOffset.get(0));
            int modelBlockSize = glm.max(Mat4x4.SIZE, uniformBufferOffset.get(0));

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
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        modelMatrixPointer = gl.glMapNamedBufferRange(
                bufferName.get(Buffer.MODEL_MATRIX),
                0,
                Mat4x4.SIZE,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    }

    private FloatBuffer getVertexBuffer(float radius, short rings, short sectors) {

        float R = 1f / (float) (rings - 1);
        float S = 1f / (float) (sectors - 1);
        short r, s;
        float x, y, z;

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(rings * sectors * (3 + 2));

        for (r = 0; r < rings; r++) {

            for (s = 0; s < sectors; s++) {

                x = glm.cos(2 * (float) glm.pi * s * S) * glm.sin((float) glm.pi * r * R);
                y = glm.sin(-(float) glm.pi / 2 + (float) glm.pi * r * R);
                z = glm.sin(2 * (float) glm.pi * s * S) * glm.sin((float) glm.pi * r * R);
                // positions
                vertexBuffer.put(x * radius);
                vertexBuffer.put(y * radius);
                vertexBuffer.put(z * radius);
                // texture coordinates
                vertexBuffer.put(1 - s * S);
                vertexBuffer.put(r * R);
            }
        }
        vertexBuffer.position(0);

        return vertexBuffer;
    }

    private ShortBuffer getElementBuffer(float radius, short rings, short sectors) {

        float R = 1f / (float) (rings - 1);
        float S = 1f / (float) (sectors - 1);
        short r, s;
        float x, y, z;

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(rings * sectors * 6);

        for (r = 0; r < rings - 1; r++) {

            for (s = 0; s < sectors - 1; s++) {

                elementBuffer.put((short) (r * sectors + s));
                elementBuffer.put((short) (r * sectors + (s + 1)));
                elementBuffer.put((short) ((r + 1) * sectors + (s + 1)));
                elementBuffer.put((short) ((r + 1) * sectors + (s + 1)));
                elementBuffer.put((short) (r * sectors + s));
//                elementBuffer.put((short) (r * sectors + (s + 1)));
                elementBuffer.put((short) ((r + 1) * sectors + s));
            }
        }
        elementBuffer.position(0);

        return elementBuffer;
    }

    private void initVertexArray(GL4 gl) {

        gl.glCreateVertexArrays(1, vertexArrayName);

        gl.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream.A);
        gl.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.TEXCOORD, Semantic.Stream.A);

        gl.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, Vec3.length, GL_FLOAT, false, 0);
        gl.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.TEXCOORD, Vec2.length, GL_FLOAT, false, Vec3.SIZE);

        gl.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);
        gl.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.TEXCOORD);

        gl.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));

        gl.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream.A, bufferName.get(Buffer.VERTEX), 0, Vec2.SIZE + Vec3.SIZE);
    }

    private void initTexture(GL4 gl) {

        try {
            URL texture = getClass().getClassLoader().getResource("images/globe.png");

            TextureData textureData = TextureIO.newTextureData(gl.getGLProfile(), texture, false, TextureIO.PNG);

            gl.glCreateTextures(GL_TEXTURE_2D, 1, textureName);

            gl.glTextureParameteri(textureName.get(0), GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTextureParameteri(textureName.get(0), GL_TEXTURE_MAX_LEVEL, 0);

            gl.glTextureStorage2D(textureName.get(0),
                    1, // level
                    textureData.getInternalFormat(),
                    textureData.getWidth(), textureData.getHeight());

            gl.glTextureSubImage2D(textureName.get(0),
                    0, // level
                    0, 0, // offset
                    textureData.getWidth(), textureData.getHeight(),
                    textureData.getPixelFormat(), textureData.getPixelType(),
                    textureData.getBuffer());

        } catch (IOException ex) {
            Logger.getLogger(HelloGlobe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initSampler(GL4 gl) {

        gl.glGenSamplers(1, samplerName);

        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        // view matrix
        {
            Mat4x4 view = glm.lookAt(new Vec3(0, 0, 3), new Vec3(), new Vec3(0, 1, 0));
            view.to(globalMatricesPointer, Mat4x4.SIZE);
        }

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1f).put(1, .5f).put(2, 0f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));


        // model matrix
        {
            long now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1_000f;

            Mat4x4 model = new Mat4x4().rotate_(-diff, 0f, 1f, 0f);
            model.to(modelMatrixPointer);
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

        gl.glBindTextureUnit(
                Semantic.Sampler.DIFFUSE,
                textureName.get(0));
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(0));

        gl.glDrawElements(
                GL_TRIANGLES,
                elementCount,
                GL_UNSIGNED_SHORT,
                0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL4 gl = drawable.getGL().getGL4();

        float aspect = (float) width / height;

        Mat4x4 proj = glm.perspective((float) glm.pi * 0.25f, aspect, 0.1f, 100f);
        proj.to(globalMatricesPointer);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        gl.glUnmapNamedBuffer(bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glUnmapNamedBuffer(bufferName.get(Buffer.MODEL_MATRIX));

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, vertexArrayName);
        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteTextures(1, textureName);
        gl.glDeleteSamplers(1, samplerName);

        destroyBuffers(vertexArrayName, bufferName, textureName, samplerName, clearColor, clearDepth);
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