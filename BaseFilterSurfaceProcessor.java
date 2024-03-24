import static androidx.core.util.Preconditions.checkState;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.DynamicRange;

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.newHandlerExecutor;
import androidx.camera.core.ProcessingException;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.processing.OpenGlRenderer;
import androidx.camera.core.processing.ShaderProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** Please check how this is used at https://github.com/androidx/androidx/blob/androidx-main/camera/integration-tests/viewtestapp/src/main/java/androidx/camera/integration/view/ToneMappingSurfaceProcessor.kt */
/** Please check how this is used at https://github.com/androidx/androidx/blob/androidx-main/camera/integration-tests/viewtestapp/src/main/java/androidx/camera/integration/view/EffectsFragment.kt */
public abstract class BaseFilterSurfaceProcessor implements SurfaceProcessor, SurfaceTexture.OnFrameAvailableListener {
    private static int TEXNAME = 1;

    protected final String LOG = getClass().getSimpleName();

    String threadName = getClass().getSimpleName() + "-Thread";
    Context context;
    HandlerThread glThread;
    Executor glExecutor;
    Handler glHandler;

    OpenGlRenderer glRenderer = new OpenGlRenderer();

    Surface surface;

    boolean outputSurfaceProvided;
    boolean isReleased;
    Map<SurfaceOutput, Surface> outputSurfaces = new HashMap<>();
    float[] textureTransform = new float[16];
    float[] surfaceTransform = new float[16];

    protected BaseFilterSurfaceProcessor(Context context) {
        Log.d(LOG, LOG + " init");
        this.context = context;
        glThread = new HandlerThread(threadName);
        glThread.start();
        glHandler = new Handler(glThread.getLooper());
        glExecutor = newHandlerExecutor(glHandler);
        glExecutor.execute(() -> {
            glRenderer.init(DynamicRange.SDR, getShaderProviderFilter());
        });
    }

    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) throws ProcessingException {
        Log.d(LOG, LOG + " onInputSurface");
        try {
            checkGlThread();
            Log.d(LOG, LOG + " onInputSurface gl thread!");

            if (isReleased) {
                Log.d(LOG, LOG + " onInputSurface released");
                surfaceRequest.willNotProvideSurface();
                Log.d(LOG, LOG + " onInputSurface released willNotProvideSurface");
                return;
            }
            Log.d(LOG, LOG + " onInputSurface not released");

            // Create Surface based on the request.
            SurfaceTexture surfaceTexture = new SurfaceTexture(TEXNAME++);
            Log.d(LOG, LOG + " onInputSurface surfaceTexture created");

            surfaceTexture.setDefaultBufferSize(
                    surfaceRequest.getResolution().getWidth(),
                    surfaceRequest.getResolution().getHeight());
            Log.d(LOG, LOG + " onInputSurface surfaceTexture defaultBufferSize set");

            surface = new Surface(surfaceTexture);
            Log.d(LOG, LOG + " onInputSurface surface created");

            // Provide the Surface to CameraX, and cleanup when it's no longer used.
            surfaceRequest.provideSurface(surface, glExecutor, result -> {
                Log.d(LOG, LOG + " provideSurface surface not needed anymore");
                surfaceTexture.setOnFrameAvailableListener(null);
                surfaceTexture.release();
                surface.release();
            });
            Log.d(LOG, LOG + " onInputSurface listening on provideSurface");

            // Listen to the incoming frames.
            surfaceTexture.setOnFrameAvailableListener(this, glHandler);
            Log.d(LOG, LOG + " onInputSurface listening setOnFrameAvailableListener");

        } catch (Exception e) {
            Log.e(LOG, LOG + " setOnFrameAvailableListener error " + e.getMessage());
            e.printStackTrace();
            release();
        }
    }

    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) throws ProcessingException {
        Log.d(LOG, LOG + " onOutputSurface");
        try {
            checkGlThread();
            Log.d(LOG, LOG + " onOutputSurface checkGlThread!");
            outputSurfaceProvided = true;
            if (isReleased) {
                Log.d(LOG, LOG + " onOutputSurface released!");
                surfaceOutput.close();
                return;
            }
            Log.d(LOG, LOG + " onOutputSurface not released");

            Surface surface = surfaceOutput.getSurface(glExecutor, (listener) -> {
                Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface");
                surfaceOutput.close();
                Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface closed");

                Surface removedSurface = outputSurfaces.remove(surfaceOutput);
                Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface removed from map");
                if (removedSurface != null) {
                    glRenderer.unregisterOutputSurface(removedSurface);
                    Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface unregisterOutputSurface");
                }
            });
            glRenderer.registerOutputSurface(surface);
            Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface registerOutputSurface");
            outputSurfaces.put(surfaceOutput, surface);
            Log.d(LOG, LOG + " onOutputSurface > surfaceOutput.getSurface added to map");
        } catch (Exception e) {
            Log.e(LOG, LOG + " onOutputSurface error " + e.getMessage());
            e.printStackTrace();
            release();
        }
    }


    public void release() {
        Log.d(LOG, LOG + " release");
        glExecutor.execute(() -> {
            releaseInternal();
        });
    }

    protected void releaseInternal() {
        Log.d(LOG, LOG + " releaseInternal");
        checkGlThread();
        if (!isReleased) {
            // Once release is called, we can stop sending frame to output surfaces.
            if (outputSurfaces != null) {
                for (SurfaceOutput surfaceOutput : outputSurfaces.keySet()) {
                    surfaceOutput.close();
                }
                outputSurfaces.clear();
            }
            glRenderer.release();
            glThread.quitSafely();
            isReleased = true;
        }
    }

    public Executor getGlExecutor() {
        return glExecutor;
    }

    private void checkGlThread() {
        Log.d(LOG, LOG + " checkGlThread");
        checkState(threadName == Thread.currentThread().getName());
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(LOG, LOG + " setOnFrameAvailableListener");
        // Process the incoming frames and draw to the output Surface from #onOutputSurface

        checkGlThread();
        Log.d(LOG, LOG + " setOnFrameAvailableListener checkGlThread");
        if (isReleased) {
            Log.d(LOG, LOG + " setOnFrameAvailableListener isReleased");
            return;
        }
        Log.d(LOG, LOG + " setOnFrameAvailableListener not released");

        surfaceTexture.updateTexImage();
        Log.d(LOG, LOG + " setOnFrameAvailableListener not updateTexImage");

        surfaceTexture.getTransformMatrix(textureTransform);
        Log.d(LOG, LOG + " setOnFrameAvailableListener not getTransformMatrix");

        for (Map.Entry<SurfaceOutput, Surface> entry : outputSurfaces.entrySet()) {
            Log.d(LOG, LOG + " setOnFrameAvailableListener surface entry");
            Surface surface = entry.getValue();
            SurfaceOutput surfaceOutput = entry.getKey();

            surfaceOutput.updateTransformMatrix(surfaceTransform, textureTransform);
            Log.d(LOG, LOG + " setOnFrameAvailableListener surface updateTransformMatrix");

            glRenderer.render(surfaceTexture.getTimestamp(), surfaceTransform, surface);
        }
    }

    protected abstract ShaderProvider getShaderProviderFilter();
}
