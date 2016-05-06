/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gl4.globe;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_MAP_INVALIDATE_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_MAP_WRITE_BIT;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
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
import framework.GlDebugOutput;
import framework.Semantic;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author elect
 */
public class Globe implements GLEventListener, KeyListener {

    private static int screenIdx = 0;
    private static Dimension windowSize = new Dimension(1024, 768);
    private static boolean undecorated = false;
    private static boolean alwaysOnTop = false;
    private static boolean fullscreen = false;
    private static boolean mouseVisible = true;
    private static boolean mouseConfined = false;
    private static String title = "texture.png";
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
        glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glWindow.setVisible(true);

        Globe globe = new Globe();
        glWindow.addGLEventListener(globe);
        glWindow.addKeyListener(globe);

        animator = new Animator(glWindow);
        animator.start();
    }

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(1);
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);
    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(new float[]{1.0f, 0.5f, 0.0f, 1.0f});
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(new float[]{1.0f});
    private FloatBuffer vertexBuffer;
    private ShortBuffer elementBuffer;
    private ByteBuffer transformPointer;
    private int programName;
    private final String SHADERS_ROOT = "src/gl4/globe/shaders";
    private final String SHADERS_NAME = "globe";
    private final String TEXTURE_ROOT = "src/gl4/globe/asset";
    private final String TEXTURE_NAME = "globe.png";
    private float[] yRotazion = new float[16], lookAt = new float[16], projection = new float[16], mvp = new float[16];
    private float[] eye = new float[]{0, 0, 3};
    private float[] center = new float[]{0, 0, 0};
    private float[] up = new float[]{0, 1, 0};
    private float aspect;
    private long start, now;
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    public Globe() {

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        initDebug(gl4);

        initBuffers(gl4);

        initVertexArray(gl4);

        initTexture(gl4);

        initSampler(gl4);

        initProgram(gl4);

        gl4.glEnable(GL4.GL_DEPTH_TEST);

        start = System.currentTimeMillis();

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        transformPointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0, 16 * Float.BYTES, GL_MAP_WRITE_BIT
                | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    }

    private void initDebug(GL4 gl4) {

        glWindow.getContext().addGLDebugListener(new GlDebugOutput());
        // Turn off all the debug
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DONT_CARE, // severity
                0, // count
                null, // id
                false); // enabled
        // Turn on all OpenGL Errors, shader compilation/linking errors, or highly-dangerous undefined behavior 
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_HIGH, // severity
                0, // count
                null, // id
                true); // enabled
        // Turn on all major performance warnings, shader compilation/linking warnings or the use of deprecated functions
        gl4.glDebugMessageControl(
                GL_DONT_CARE, // source
                GL_DONT_CARE, // type
                GL_DEBUG_SEVERITY_MEDIUM, // severity
                0, // count
                null, // id
                true); // enabled
    }

    private void initBuffers(GL4 gl4) {

        createGeometry(1, (short) 100, (short) 100);

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.VERTEX), vertexBuffer.capacity() * Float.BYTES, vertexBuffer,
                    GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.ELEMENT), elementBuffer.capacity() * Short.BYTES,
                    elementBuffer, GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.TRANSFORM), 16 * Float.BYTES, null, GL_MAP_WRITE_BIT);

        } else {
            // vertices
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                gl4.glBufferStorage(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, 0);
            }
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);
            // elements
            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            {
                gl4.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, 0);
            }
            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            // transform
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            {
                IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
                gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
                int uniformBlockSize = Math.max(16 * Float.BYTES, uniformBufferOffset.get(0));

                gl4.glBufferStorage(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT
                        | GL_MAP_COHERENT_BIT);

                BufferUtils.destroyDirectBuffer(uniformBufferOffset);
            }
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(elementBuffer);
    }

    private void createGeometry(float radius, short rings, short sectors) {

        float R = 1f / (float) (rings - 1);
        float S = 1f / (float) (sectors - 1);
        short r, s;
        float x, y, z;

        vertexBuffer = GLBuffers.newDirectFloatBuffer(rings * sectors * (3 + 2));

        for (r = 0; r < rings; r++) {

            for (s = 0; s < sectors; s++) {

                x = (float) (Math.cos(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));
                y = (float) Math.sin(-Math.PI / 2 + Math.PI * r * R);
                z = (float) (Math.sin(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));
                // positions
                vertexBuffer.put(x * radius);
                vertexBuffer.put(y * radius);
                vertexBuffer.put(z * radius);
                // texture coordinates
                vertexBuffer.put(1 - s * S);
                vertexBuffer.put(r * R);
            }
        }
        vertexBuffer.rewind();

        elementBuffer = GLBuffers.newDirectShortBuffer(rings * sectors * 6);

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
        elementBuffer.rewind();
    }

    private void initVertexArray(GL4 gl4) {

        gl4.glCreateVertexArrays(1, vertexArrayName);

        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.POSITION, Semantic.Stream._0);
        gl4.glVertexArrayAttribBinding(vertexArrayName.get(0), Semantic.Attr.TEXCOORD, Semantic.Stream._0);

        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.POSITION, 3, GL_FLOAT, false, 0);
        gl4.glVertexArrayAttribFormat(vertexArrayName.get(0), Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false,
                3 * Float.BYTES);
        
        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.POSITION);
        gl4.glEnableVertexArrayAttrib(vertexArrayName.get(0), Semantic.Attr.TEXCOORD);

        gl4.glVertexArrayElementBuffer(vertexArrayName.get(0), bufferName.get(Buffer.ELEMENT));
        gl4.glVertexArrayVertexBuffer(vertexArrayName.get(0), Semantic.Stream._0, bufferName.get(Buffer.VERTEX), 0,
                (3 + 2) * Float.BYTES);
    }

    private void initTexture(GL4 gl4) {

        try {
            File textureFile = new File(TEXTURE_ROOT + "/" + TEXTURE_NAME);

            TextureData textureData = TextureIO.newTextureData(gl4.getGLProfile(), textureFile, false, TextureIO.PNG);

            gl4.glCreateTextures(GL_TEXTURE_2D, 1, textureName);

            gl4.glTextureParameteri(textureName.get(0), GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTextureParameteri(textureName.get(0), GL_TEXTURE_MAX_LEVEL, 0);

            gl4.glTextureStorage2D(textureName.get(0),
                    1, // level
                    textureData.getInternalFormat(),
                    textureData.getWidth(), textureData.getHeight());

            gl4.glTextureSubImage2D(textureName.get(0),
                    0, // level
                    0, 0, // offset
                    textureData.getWidth(), textureData.getHeight(), // size
                    textureData.getPixelFormat(), textureData.getPixelType(), // format and type
                    textureData.getBuffer()); // data

        } catch (IOException ex) {
            Logger.getLogger(Globe.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initSampler(GL4 gl4) {

        FloatBuffer black = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 1.0f});

        gl4.glCreateSamplers(1, samplerName);

        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        gl4.glSamplerParameterfv(samplerName.get(0), GL_TEXTURE_BORDER_COLOR, black);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MIN_LOD, -1000.f);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MAX_LOD, 1000.f);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_LOD_BIAS, 0.0f);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_MODE, GL_NONE);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        gl4.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MAX_ANISOTROPY_EXT, 1.0f);
        gl4.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_CUBE_MAP_SEAMLESS, GL_TRUE);

        BufferUtils.destroyDirectBuffer(black);
    }

    private void initProgram(GL4 gl4) {

        ShaderCode vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_NAME, "vert", null, true);
        ShaderCode fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_NAME, "frag", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl4);

        programName = shaderProgram.program();

        shaderProgram.link(gl4, System.out);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glClearBufferfv(GL_COLOR, 0, clearColor);
        gl4.glClearBufferfv(GL_DEPTH, 0, clearDepth);

        {
            now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1000;

            FloatUtil.makeLookAt(lookAt, 0, eye, 0, center, 0, up, 0, yRotazion);
            yRotazion = FloatUtil.makeRotationEuler(yRotazion, 0, 0, -diff, 0);
            FloatUtil.makePerspective(projection, 0, true, (float) Math.PI * 0.25f, aspect, 0.1f, 100f);

            FloatUtil.multMatrix(projection, lookAt, mvp); // mvp = projection * lookAt
            FloatUtil.multMatrix(mvp, yRotazion); // mvp *= yRotation

            transformPointer.asFloatBuffer().put(mvp);
        }

        gl4.glUseProgram(programName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBufferBase(
                GL_UNIFORM_BUFFER, // target
                Semantic.Uniform.TRANSFORM0, // index 
                bufferName.get(Buffer.TRANSFORM)); // buffer

        gl4.glBindTextureUnit(
                Semantic.Sampler.DIFFUSE, // texture unit
                textureName.get(0)); // texture name
        gl4.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(0));

        gl4.glDrawElements(
                GL_TRIANGLES, // primitive mode
                elementBuffer.capacity(), // element count
                GL_UNSIGNED_SHORT, // element type
                0); // element offset
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.out.println("reshape");
        GL4 gl4 = drawable.getGL().getGL4();
        gl4.glViewport(x, y, width, height);
        aspect = (float) windowSize.getWidth() / windowSize.getHeight();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glUnmapNamedBuffer(bufferName.get(Buffer.TRANSFORM));

        gl4.glDeleteProgram(programName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteTextures(1, textureName);
        gl4.glDeleteSamplers(1, samplerName);

        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(samplerName);

        BufferUtils.destroyDirectBuffer(clearColor);
        BufferUtils.destroyDirectBuffer(clearDepth);

        System.exit(0);
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
