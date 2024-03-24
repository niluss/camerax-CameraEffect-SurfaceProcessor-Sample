import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.processing.ShaderProvider;

public class BwFilter extends BaseFilterSurfaceProcessor {

    public BwFilter(Context context) {
        super(context);
    }

    protected ShaderProvider getShaderProviderFilter() {
        return new ShaderProvider() {
            @Nullable
            @Override
            public String createFragmentShader(@NonNull String samplerVarName, @NonNull String fragCoordsVarName) {
                String program = "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "uniform samplerExternalOES $samplerVarName;\n" +
                        "varying vec2 $fragCoordsVarName;\n" +
                        "void main() {\n" +
                        "    vec4 c = texture2D($samplerVarName, $fragCoordsVarName);\n" +
                        "    float avg = (c.r + c.g + c.b) / 3.0;\n" +
                        "    gl_FragColor = vec4(avg, avg, avg, 1.0);\n" +
                        "}";
                program = program.replaceAll("\\$samplerVarName", samplerVarName);
                program = program.replaceAll("\\$fragCoordsVarName", fragCoordsVarName);
                Log.e(LOG, LOG + "program:\n" + program);
                return program;
            }
        };
    }
}
