package com.smibii.flashables.client.render;

import com.smibii.flashables.light.LightManager;
import com.smibii.flashables.light.types.PointLight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class LightBuffer {
    public static final int MAX_LIGHTS = 128;

    /*
     * One light = 64 bytes / 16 floats.
     *
     * The layout is:
     *
     *  0  position.x
     *  1  position.y
     *  2  position.z
     *  3  radius
     *
     *  4  color.r
     *  5  color.g
     *  6  color.b
     *  7  intensity
     *
     *  8  direction.x
     *  9  direction.y
     * 10  direction.z
     * 11  type
     *
     * 12  innerCos
     * 13  outerCos
     * 14  textureId
     * 15  padding
     */
    private static final int FLOATS_PER_LIGHT = 16;

    private static final int BYTES_PER_LIGHT =
            FLOATS_PER_LIGHT * Float.BYTES;

    private static final int BUFFER_SIZE =
            MAX_LIGHTS * BYTES_PER_LIGHT;

    private static final ByteBuffer CPU_BUFFER =
            ByteBuffer
                    .allocateDirect(BUFFER_SIZE)
                    .order(ByteOrder.nativeOrder());

    private static int bufferId = 0;

    private static int lightCount = 0;

    private LightBuffer() {
    }

    public static void init() {

        if (bufferId != 0) {
            return;
        }

        bufferId =
                GL15.glGenBuffers();

        GL15.glBindBuffer(
                GL31.GL_UNIFORM_BUFFER,
                bufferId
        );

        GL15.glBufferData(
                GL31.GL_UNIFORM_BUFFER,
                BUFFER_SIZE,
                GL15.GL_DYNAMIC_DRAW
        );

        GL15.glBindBuffer(
                GL31.GL_UNIFORM_BUFFER,
                0
        );
    }

    public static void update() {

        if (bufferId == 0) {
            init();
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            lightCount = 0;
            return;
        }

        Vec3 cameraPosition =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        List<PointLight> lights =
                LightManager.getVisibleLights(
                        cameraPosition,
                        MAX_LIGHTS
                );

        CPU_BUFFER.clear();

        lightCount = lights.size();

        for (PointLight light : lights) {

            GPULight gpu =
                    GPULight.from(light);

            CPU_BUFFER.putFloat(gpu.x());
            CPU_BUFFER.putFloat(gpu.y());
            CPU_BUFFER.putFloat(gpu.z());
            CPU_BUFFER.putFloat(gpu.radius());

            CPU_BUFFER.putFloat(gpu.r());
            CPU_BUFFER.putFloat(gpu.g());
            CPU_BUFFER.putFloat(gpu.b());
            CPU_BUFFER.putFloat(gpu.intensity());

            CPU_BUFFER.putFloat(gpu.dx());
            CPU_BUFFER.putFloat(gpu.dy());
            CPU_BUFFER.putFloat(gpu.dz());
            CPU_BUFFER.putFloat(gpu.type());

            CPU_BUFFER.putFloat(gpu.innerCos());
            CPU_BUFFER.putFloat(gpu.outerCos());
            CPU_BUFFER.putFloat(gpu.textureId());
            CPU_BUFFER.putFloat(gpu.padding());
        }

        CPU_BUFFER.flip();

        GL15.glBindBuffer(
                GL31.GL_UNIFORM_BUFFER,
                bufferId
        );

        GL15.glBufferSubData(
                GL31.GL_UNIFORM_BUFFER,
                0,
                CPU_BUFFER
        );

        GL15.glBindBuffer(
                GL31.GL_UNIFORM_BUFFER,
                0
        );
    }

    public static void bind(int bindingPoint) {

        if (bufferId == 0) {
            return;
        }

        GL30.glBindBufferBase(
                GL31.GL_UNIFORM_BUFFER,
                bindingPoint,
                bufferId
        );
    }

    public static int getLightCount() {
        return lightCount;
    }

    public static int getBufferId() {
        return bufferId;
    }

    public static void destroy() {

        if (bufferId == 0) {
            return;
        }

        GL15.glDeleteBuffers(bufferId);

        bufferId = 0;
    }
}