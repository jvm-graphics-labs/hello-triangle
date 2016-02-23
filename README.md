This project, in pure jogl, contains the following:

### [GL3 HelloTriangle](https://github.com/elect86/helloTriangle/tree/master/HelloTriangle/src/gl3/helloTriangle) 

<a href="url"><img src="http://i.imgur.com/i22AI9I.png" width="200" ></a>

A rotating triangle featuring: 
- GL3
- pure newt
- glWindow options
- animator
- indexed drawing
- dynamic attribute binding
- dynamic frag data binding
- interleaved data
- vertex array object (VAO)
- vertex buffer object (VBO) with normalization on one attribute
- index buffer object (IBO) with shorts
- uniform
- glsl program (with specific suffix) jogl util 
- matrix jogl util 
- gl error check
- key listener 
- right way to dispose

### [GL3 HelloTexture](https://github.com/elect86/helloTriangle/tree/master/HelloTriangle/src/gl3/helloTexture)

<a href="url"><img src="http://i.imgur.com/HbnqqX5.png" width="200" ></a> 

A rotating square with a texture

### [GL4 Globe](https://github.com/elect86/helloTriangle/blob/master/HelloTriangle/src/gl4/globe/Globe.java)

<a href="url"><img src="http://i.imgur.com/0NqgdcP.png" width="200" ></a> 

Quick start:

* download [`jogamp-all-platforms.7z`](https://jogamp.org/deployment/jogamp-current/archive/jogamp-all-platforms.7z) and extract it wherever you like. This is the place where the jogl dependencies will be searched.
* set up your [favourite IDE](https://jogamp.org/wiki/index.php/Setting_up_a_JogAmp_project_in_your_favorite_IDE) creating a library and pointing it to `jogamp-all-platforms\jar\gluegen-rt.jar` and `jogamp-all-platforms\jar\jogl-all.jar`. You are stronly advised to not move those jar since they need to lie in the same place as the natives of your platform are.
* clone, download, copy/paste the code or do whatever you like with the code of the samples.
* run it and enjoy the OpenGL acceleration on java  :sunglasses:

If you have any problem/question/doubt do not hesitate asking on the [jogl forums](http://forum.jogamp.org/) or [StackOverflow](http://stackoverflow.com/) or open an [issue here](https://github.com/elect86/helloTriangle/issues)

