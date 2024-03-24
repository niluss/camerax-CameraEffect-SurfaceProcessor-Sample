# camerax_CameraEffect_SurfaceProcessor_sample
CameraEffect SurfaceProcessor Example
by Lynnus N. Tan

- This is Java example snippet based on androidx ToneMappingSurfaceProcessor.kt and EffectsFragment.kt to add a black and white CameraEffect to ImageCapture, VideCapture and Preview using OpenGlRenderer and fragment shader.
- This is not a fully working example, and you have to insert it in your code.

- This is in Java 1.8 source and target
- This is tested on minSdk 31 and targetSdkVersion 34
- There will be warnings like "can only be called from within the same library (androidx.camera:camera-core)" but it will build and run fine. I don't understand this myself why the Classes and methods are not accessible.

See the following files:
- code_to_bindToLifecycle.txt file for the snippet to instantiate the effect and add via bindToLifecycle
- BaseFilterSurfaceProcessor.java for OpenGlRenderer related code
- BwFilter.java for the implementation of the fragment shader. You can create more classes like this
- CameraEffectProxy.java 
