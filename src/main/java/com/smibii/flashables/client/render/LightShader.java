package com.smibii.flashables.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.smibii.flashables.Flashables;
import com.smibii.flashables.client.FlashlightTest;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public class LightShader {
    private final int program;

    private final int modelViewLocation;
    private final int projectionLocation;
    private final int inverseProjectionLocation;

    private final int lightPositionLocation;
    private final int lightDirectionLocation;

    private final int lightColorLocation;

    private final int lightIntensityLocation;
    private final int lightRadiusLocation;

    private final int innerCosLocation;
    private final int outerCosLocation;

    private final int screenSizeLocation;

    private final int depthSamplerLocation;
    private final int lightTextureLocation;

    private LightShader(int program) {

        this.program = program;

        modelViewLocation =
                uniform("ModelViewMat");

        projectionLocation =
                uniform("ProjMat");

        inverseProjectionLocation =
                uniform("InvProjMat");

        lightPositionLocation =
                uniform("LightPosition");

        lightDirectionLocation =
                uniform("LightDirection");

        lightColorLocation =
                uniform("LightColor");

        lightIntensityLocation =
                uniform("LightIntensity");

        lightRadiusLocation =
                uniform("LightRadius");

        innerCosLocation =
                uniform("InnerCos");

        outerCosLocation =
                uniform("OuterCos");

        screenSizeLocation =
                uniform("ScreenSize");

        depthSamplerLocation =
                uniform("DepthSampler");

        lightTextureLocation =
                uniform("LightTexture");
    }

    public static LightShader createPointShader() {

        String vertex =
                read(
                        Flashables.location("shaders/dynamic_lighting/point_light.vsh")
                );

        String fragment =
                read(
                        Flashables.location("shaders/dynamic_lighting/point_light.fsh")
                );

        return new LightShader(
                createProgram(
                        vertex,
                        fragment
                )
        );
    }

    public static LightShader createSpotShader() {

        String vertex =
                read(
                        Flashables.location("shaders/dynamic_lighting/spot_light.vsh")
                );

        String fragment =
                read(
                        Flashables.location("shaders/dynamic_lighting/spot_light.fsh")
                );

        return new LightShader(
                createProgram(
                        vertex,
                        fragment
                )
        );
    }

    private static int createProgram(
            String vertexSource,
            String fragmentSource
    ) {

        int vertexShader =
                compile(
                        GL20.GL_VERTEX_SHADER,
                        vertexSource
                );

        int fragmentShader =
                compile(
                        GL20.GL_FRAGMENT_SHADER,
                        fragmentSource
                );

        int program =
                GL20.glCreateProgram();

        GL20.glAttachShader(
                program,
                vertexShader
        );

        GL20.glAttachShader(
                program,
                fragmentShader
        );

        GL20.glBindAttribLocation(
                program,
                0,
                "Position"
        );

        GL20.glLinkProgram(
                program
        );

        if (
                GL20.glGetProgrami(
                        program,
                        GL20.GL_LINK_STATUS
                ) == GL20.GL_FALSE
        ) {

            String log =
                    GL20.glGetProgramInfoLog(
                            program
                    );

            GL20.glDeleteProgram(
                    program
            );

            GL20.glDeleteShader(
                    vertexShader
            );

            GL20.glDeleteShader(
                    fragmentShader
            );

            throw new IllegalStateException(
                    "Dynamic Lighting shader link failed:\n" +
                            log
            );
        }

        GL20.glDetachShader(
                program,
                vertexShader
        );

        GL20.glDetachShader(
                program,
                fragmentShader
        );

        GL20.glDeleteShader(
                vertexShader
        );

        GL20.glDeleteShader(
                fragmentShader
        );

        return program;
    }

    private static int compile(
            int type,
            String source
    ) {

        int shader =
                GL20.glCreateShader(type);

        GL20.glShaderSource(
                shader,
                source
        );

        GL20.glCompileShader(
                shader
        );

        if (
                GL20.glGetShaderi(
                        shader,
                        GL20.GL_COMPILE_STATUS
                ) == GL20.GL_FALSE
        ) {

            String log =
                    GL20.glGetShaderInfoLog(
                            shader
                    );

            GL20.glDeleteShader(
                    shader
            );

            throw new IllegalStateException(
                    "Dynamic Lighting shader compilation failed:\n" +
                            log +
                            "\n\nSource:\n" +
                            source
            );
        }

        return shader;
    }

    private int uniform(
            String name
    ) {

        return GL20.glGetUniformLocation(
                program,
                name
        );
    }

    public void bind() {

        RenderSystem.assertOnRenderThread();

        GL20.glUseProgram(
                program
        );
    }

    public void unbind() {

        GL20.glUseProgram(0);
    }

    public void setModelView(
            Matrix4f matrix
    ) {

        if (modelViewLocation < 0) {
            return;
        }

        FloatBuffer buffer =
                memAllocFloat(16);

        try {

            matrix.get(buffer);

            GL20.glUniformMatrix4fv(
                    modelViewLocation,
                    false,
                    buffer
            );

        } finally {

            memFree(buffer);
        }
    }

    public void setProjection(
            Matrix4f matrix
    ) {

        if (projectionLocation < 0) {
            return;
        }

        FloatBuffer buffer =
                memAllocFloat(16);

        try {

            matrix.get(buffer);

            GL20.glUniformMatrix4fv(
                    projectionLocation,
                    false,
                    buffer
            );

        } finally {

            memFree(buffer);
        }
    }

    public void setInverseProjection(
            Matrix4f matrix
    ) {

        if (inverseProjectionLocation < 0) {
            return;
        }

        FloatBuffer buffer =
                memAllocFloat(16);

        try {

            matrix.get(buffer);

            GL20.glUniformMatrix4fv(
                    inverseProjectionLocation,
                    false,
                    buffer
            );

        } finally {

            memFree(buffer);
        }
    }

    public void setLightPosition(
            float x,
            float y,
            float z
    ) {

        if (lightPositionLocation >= 0) {

            GL20.glUniform3f(
                    lightPositionLocation,
                    x,
                    y,
                    z
            );
        }
    }

    public void setLightDirection(
            float x,
            float y,
            float z
    ) {

        if (lightDirectionLocation >= 0) {

            GL20.glUniform3f(
                    lightDirectionLocation,
                    x,
                    y,
                    z
            );
        }
    }

    public void setLightColor(
            float r,
            float g,
            float b
    ) {

        if (lightColorLocation >= 0) {

            GL20.glUniform3f(
                    lightColorLocation,
                    r,
                    g,
                    b
            );
        }
    }

    public void setIntensity(
            float intensity
    ) {

        if (lightIntensityLocation >= 0) {

            GL20.glUniform1f(
                    lightIntensityLocation,
                    intensity
            );
        }
    }

    public void setRadius(
            float radius
    ) {

        if (lightRadiusLocation >= 0) {

            GL20.glUniform1f(
                    lightRadiusLocation,
                    radius
            );
        }
    }

    public void setInnerCos(
            float value
    ) {

        if (innerCosLocation >= 0) {

            GL20.glUniform1f(
                    innerCosLocation,
                    value
            );
        }
    }

    public void setOuterCos(
            float value
    ) {

        if (outerCosLocation >= 0) {

            GL20.glUniform1f(
                    outerCosLocation,
                    value
            );
        }
    }

    public void setScreenSize(
            float width,
            float height
    ) {

        if (screenSizeLocation >= 0) {

            GL20.glUniform2f(
                    screenSizeLocation,
                    width,
                    height
            );
        }
    }

    public void setDepthSampler(
            int textureUnit
    ) {

        if (depthSamplerLocation >= 0) {

            GL20.glUniform1i(
                    depthSamplerLocation,
                    textureUnit
            );
        }
    }

    public void setLightTexture(
            int textureUnit
    ) {

        if (lightTextureLocation >= 0) {

            GL20.glUniform1i(
                    lightTextureLocation,
                    textureUnit
            );
        }
    }

    public int getProgram() {
        return program;
    }

    public void destroy() {

        if (program != 0) {

            GL20.glDeleteProgram(
                    program
            );
        }
    }

    private static String read(
            ResourceLocation location
    ) {

        try {

            InputStream stream =
                    net.minecraft.client.Minecraft
                            .getInstance()
                            .getResourceManager()
                            .getResourceOrThrow(location)
                            .open();

            try (stream) {

                return new String(
                        stream.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load shader: " +
                            location,
                    e
            );
        }
    }
}
