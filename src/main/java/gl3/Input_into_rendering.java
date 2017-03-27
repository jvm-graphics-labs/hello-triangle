package gl3;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2ES3.GL_COLOR;

/**
 * Created by GBarbieri on 27.03.2017.
 */
public class Input_into_rendering implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new Input_into_rendering().setup();
    }

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(new float[]{0, 0, 0, 0});

    private NEWTEventFiFo eventFiFo = new NEWTEventFiFo();

    private void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Input into rendering");
        window.setSize(1024, 768);

        window.addGLEventListener(this);
        window.addKeyListener(this);

        window.setVisible(true);

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
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        NEWTEvent event = eventFiFo.get();
        while (event != null) {
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                if (keyEvent.getEventType() == KeyEvent.EVENT_KEY_PRESSED)
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.VK_R:
                            modified(0);
                            break;
                        case KeyEvent.VK_G:
                            modified(1);
                            break;
                        case KeyEvent.VK_B:
                            modified(2);
                            break;
                        case KeyEvent.VK_A:
                            modified(3);
                            break;
                    }
            }
            event = eventFiFo.get();
        }

        gl.glClearBufferfv(GL_COLOR, 0, clearColor);
    }

    private void modified(int index) {
        clearColor.put(index, (clearColor.get(index) == 0) ? 1 : 0);
        System.out.println("clear color: (" + clearColor.get(0) + ", " + clearColor.get(1) + ", " + clearColor.get(2) + ", " + clearColor.get(3) + ")");
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            new Thread(() -> {
                window.destroy();
            }).start();

        eventFiFo.put(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        eventFiFo.put(e);
    }
}
