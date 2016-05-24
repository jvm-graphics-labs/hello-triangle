# Wiki

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

private float[] scale = new float[16];
private float[] zRotazion = new float[16];
private float[] modelToClip = new float[16];
private long start, now;
```
`programName` is the variable holding the program (shader) name (int). `modelToClipMatrixUL` holds the uniform 
location (UL) of the `modelToClip` matrix that transforms the vertices from the Model to the Clip Space. 
Actually there are other two spaces in between, the World and the Camera(View) Space. In theory it is like this:

Model Space * modelToWorld matrix = World Space * worldToCamera/View matrix = Camera/View Space 
* camera/viewToClip (projection) matrix -> Clip Space

<a href="url"><img src="http://web.archive.org/web/20140106105946/http://www.arcsynthesis.org/gltut/Positioning/TransformPipeline.svg"></a>

but here we keep it simple and use just a matrix that transforms vertices directly form Model to Clip Space.
Why we declare them here instead inside our rendering loop (the `display()` method)? Because we try to keep
the allocation of new objects lowest as possible, so we allocated them just once at the begin and then re-use
them every time.

`SHADERS_ROOT` represent the location where the our shaders are located.

`scale` and `zRotation` will be multiplied together and will be our `modelToClip` model. `start` and `now` 
will hold timing measurements, useful to animate our triangle and make it rotate based on time.
```java
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
```
the `init()` is called by the `animator` only once at the begin, here one usually initializes all the opengl
resources, vertex buffer object (VBO), index/element buffer object (IBO), vertex array object (VAO) and shader
program. `GL_DEPTH_TEST` says to OpenGL that while it renders object by object, it has to overwrite what has
been already rendered if their depth is bigger than the one being rendered at the moment. This is totally 
fine if you have only opaque objects, but if you have transparent ones then things get complicate and you need
to implement specific algorithms. `System.currentTimeMillis()` stores the current time (in milliseconds) 
inside `start`.

```java
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
``` 

Here the vertex and the element array buffer objects get initialized. We generated all the buffers with `glGenBuffers` and then bind each of them and upload the data. At the end, we have to manually destroy the direct buffers since they are allocations outside the java domain.

```java
    private void initVertexArray(GL3 gl3) {
    
        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
        
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                int stride = (2 + 3) * Float.BYTES;
                int offset = 0 * Float.BYTES;
                
                gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, stride, offset);
                
                offset = 2 * Float.BYTES;
                gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
                gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL_FLOAT, false, stride, offset);
            }
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl3.glBindVertexArray(0);

        checkError(gl3, "initVao");
    }
```

    
The Vertex Array is one object that store vertices properties and binding in order to avoid the users type all those stuff continuously in the render loop. `glGenVertexArrays` generates the VAO and `glBindVertexArray` binds it. From this point on, we will modify the VAO status. Vertex buffers are not part of vertex array, so we need it to bind it only when we call`glEnableVertexAttribArray` and `glVertexAttribPointer`, so that VAO saves which vertex attributes are enabled, their layout and which vertex buffer they refer to. After that, we can unbind the VBO.
Since our layout (for each vertex) is the following:
    
[position x | position y | color R | color G | color B]
    
We have to enable the position attribute with `glEnableVertexAttribArray(Semantic.Attr.POSITION)` and set the layout with `glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, stride, offset);` where:

- `Semantic.Attr.POSITION` is the index of the vertex attribute to define, 
- `2` is the number of component (x, y), 
- `GL_FLOAT` is the type of data we are passing, 
- `false` tell OpenGL to not normalized it, 
- `stride` is the total size of the vertex, that is `(2 + 3) * Float.BYTES;`,
- offset is the offset (in bytes) of the `Semantic.Attr.POSITION` vertex attribute inside each vertex

How will OpenGL know which VBO shall it fetch the data from? The one bound at `GL_ARRAY_BUFFER` at the moment you call `glVertexAttribPointer` is the answer.

In case you wonder if the call order between `glEnableVertexAttribArray` and `glVertexAttribPointer` may matter, don't worry, it doesn't.

Then, we do the same for the `Semantic.Attr.COLOR`, passing 3 instead 2 (r, g, b) and `2 * Float.BYTES` as offset because the color attribute comes after the position.
The element array buffer instead is part of the VAO, so we have to bind it.
Now we have terminate to set up our VAO and can finally unbind it with `glBindVertexArray(0);`.
