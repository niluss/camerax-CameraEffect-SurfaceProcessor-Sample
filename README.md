# Camerax CameraEffect SurfaceProcessor sample using OpenGL/GLSL (Java)
CameraEffect SurfaceProcessor Example using OpenGL/GLSL Fragment Shader in Java
by Lynnus N. Tan

- This is Java example snippet based on androidx ToneMappingSurfaceProcessor.kt and EffectsFragment.kt to add a black and white CameraEffect to ImageCapture, VideCapture and Preview using OpenGlRenderer and fragment shader.
- This is not a fully working example, and you have to insert it in your code.

- This is in Java 1.8 source and target
- This is tested on minSdk 31, targetSdkVersion 34, and CameraX version 1.3.2
- There will be warnings like "can only be called from within the same library (androidx.camera:camera-core)" but it will build and run fine. I don't understand this myself why the Classes and methods are not accessible.

See the following files:
- code_to_bindToLifecycle.txt file for the snippet to instantiate the effect and add via bindToLifecycle
- BaseFilterSurfaceProcessor.java for OpenGlRenderer related code
- BwFilter.java for the implementation of the fragment shader. You can create more classes like this
- CameraEffectProxy.java 

Observations:
- If I bind all Preview, ImageCapture and Video, I get a cropped view/capture. The effect works on all 3 but what I see is cropped.
- If I bind only Preview and InageCapture, then I see the full image (you still need to target the 3, PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE).
