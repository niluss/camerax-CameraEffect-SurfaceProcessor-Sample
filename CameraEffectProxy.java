import androidx.annotation.NonNull;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceProcessor;
import androidx.core.util.Consumer;

import java.util.concurrent.Executor;

/**
* This file is needed because we can't instantiate CameraEffect directly
*/
public class CameraEffectProxy extends CameraEffect {
    public CameraEffectProxy(int targets, @NonNull Executor executor, @NonNull SurfaceProcessor surfaceProcessor, @NonNull Consumer<Throwable> errorListener) {
        super(targets, executor, surfaceProcessor, errorListener);
    }

}
