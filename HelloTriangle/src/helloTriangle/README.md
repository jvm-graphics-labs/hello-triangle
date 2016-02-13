# Explanation

The `main` function initializes different parameters and instantiates the `HelloTriangle` class
```java
Display display = NewtFactory.createDisplay(null);
Screen screen = NewtFactory.createScreen(display, screenIdx);
GLProfile glProfile = GLProfile.get(GLProfile.GL4);
GLCapabilities glCapabilities = new GLCapabilities(glProfile);
glWindow = GLWindow.create(screen, glCapabilities);
```
Most of these calls are standard, you should't change them unless you know what you do/need, the important one is the one that retrieves the `GLProfile`. Here we choose to get a `GL4` core, if we want a compatibility profile `GL4bc` that does not mean Before Christus :smile: but Backward Compatibility. `GL4ES3` stands for an intersection of GL4 and ES3.
```java
glWindow.setSize(windowSize.getWidth(), windowSize.getHeight());
glWindow.setPosition(50, 50);
glWindow.setUndecorated(undecorated);
glWindow.setAlwaysOnTop(alwaysOnTop);
glWindow.setFullscreen(fullscreen);
glWindow.setPointerVisible(mouseVisible);
glWindow.confinePointer(mouseConfined);
glWindow.setTitle(title);
glWindow.setVisible(true);
```
these are pretty self-explanatory, normally one does not set all of them because the default values are just fine, but if one needs something specifically he can set it.
```java
HelloTriangle helloTriangle = new HelloTriangle();
glWindow.addGLEventListener(helloTriangle);
glWindow.addKeyListener(helloTriangle);

animator = new Animator(glWindow);
animator.start();
```
`HelloTriangle` instantiation and since it implements `GLEventListener, KeyListener` it has to be added to our `glWindow` both as a listener for the opengl and user actions. The `animator` takes care of updating the image by calling continuosly the `display()` method.
```java
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
```
Simple helper classes to write/read OpenGL objects from our `IntBuffer objectsName`. OpenGL handles objects based on name (integer values). These classes make the code more readable, easier to understand and decrease the possibility of errors. Strongly suggested.
```java
private byte[] vertexData = new byte[]{
    (byte) -1, (byte) -1, Byte.MAX_VALUE, (byte) 0, (byte) 0,
    (byte) +0, (byte) +2, (byte) 0, (byte) 0, Byte.MAX_VALUE,
    (byte) +1, (byte) -1, (byte) 0, Byte.MAX_VALUE, (byte) 0
};
private short[] indexData = new short[]{
    0, 2, 1
};
```
`vertexData` is the array where all the vertex attributes are stored. Each line represents an interleaved vertex attribute and they are counted starting from zero. This means the first line are the vertex attributes for vertex 0, the second vertex 1 and the last one vertex 2. Usually floats are used, but in this sample I went with bytes. 
They can be separated in different buffers, contiguous or interleaved.
Separated:
* buffer0 -> [position0, position1, ...]
* buffer1 -> [color0, color1, ...]

Contiguous:
* buffer0 -> [position0, positio1, ..., color0, color1, ...]

Interleaved:
* buffer0 -> [position0, color0, position1, color1, ...]
 

Interleaved is the best option since it exploits data locality and this is the option is used here. For each line, the first two bytes indicates the position while the last four the color of the i-th vertex. Take in account the positions are going to be used as they are but the colors are going to be instead normalized. Since I am going to load them as signed bytes they will be resolved in the range [-1, 1]. That is, in the shaders, a `Byte.MAX_VALUE` will correspond to a value equal to 1.
How many vertices can you store? You cannot know a priori. But you will hit the limit once you get a `GL_OUT_OF_MEMORY` error.

`indexData` is instead the array containing the indices. We choose element/index rendering, this means OpenGL will fetch the vertices attributes from the `vertexData` based on the supplied indices, that is vertex 0, vertex 2 and then vertex 1.
```java
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
```
`programName` is the variable holding the program (shader) name (int). `
