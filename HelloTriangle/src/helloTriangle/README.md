# Explanation

The `main` function initializes different parameters and instantiates the `HelloTriangle` class
```java
Display display = NewtFactory.createDisplay(null);
Screen screen = NewtFactory.createScreen(display, screenIdx);
GLProfile glProfile = GLProfile.get(GLProfile.GL4);
GLCapabilities glCapabilities = new GLCapabilities(glProfile);
glWindow = GLWindow.create(screen, glCapabilities);
```
Most of the calls are standard, you should't change them, the important one is the one that retrieves the `GLProfile`. Here we choose to get a `GL4` core, if we want a compatibility profile `GL4bc` that does not mean Before Christus :smile: but Backward Compatibility. `GL4ES3` stands for an intersection of GL4 and ES3.
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
these call are pretty self-explanatory, normally one does not set all of them because the default values are just fine, but if one needs something specifically he can set it.
```java
HelloTriangle helloTriangle = new HelloTriangle();
glWindow.addGLEventListener(helloTriangle);
glWindow.addKeyListener(helloTriangle);

animator = new Animator(glWindow);
animator.start();
```
`HelloTriangle` instantiation and since it implements `GLEventListener, KeyListener` it has to be added to our `glWindow` both as a listener for the opengl and user actions. The `animator` takes care of updating the image by calling continuosly the `display()` method.
