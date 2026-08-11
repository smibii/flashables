package com.smibii.flashables.client.render.mesh;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public class LightMesh {
    protected int vao;
    protected int vbo;
    protected int vertexCount;

    protected LightMesh() {
    }

    protected final void createMesh(float[] vertices) {

        vertexCount = vertices.length / 3;

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();

        GL30.glBindVertexArray(vao);

        GL15.glBindBuffer(
                GL15.GL_ARRAY_BUFFER,
                vbo
        );

        FloatBuffer buffer =
                memAllocFloat(vertices.length);

        try {
            buffer.put(vertices);
            buffer.flip();

            GL15.glBufferData(
                    GL15.GL_ARRAY_BUFFER,
                    buffer,
                    GL15.GL_STATIC_DRAW
            );
        } finally {
            memFree(buffer);
        }

        /*
         * Position = location 0
         *
         * vec3 Position
         */
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(
                0,
                3,
                GL20.GL_FLOAT,
                false,
                3 * Float.BYTES,
                0
        );

        GL15.glBindBuffer(
                GL15.GL_ARRAY_BUFFER,
                0
        );

        GL30.glBindVertexArray(0);
    }

    public final void bind() {

        GL30.glBindVertexArray(
                vao
        );
    }

    public final void unbind() {

        GL30.glBindVertexArray(0);
    }

    public final void draw() {

        GL11Compat.drawArrays(
                vertexCount
        );
    }

    public final int getVertexCount() {
        return vertexCount;
    }

    public final void destroy() {

        if (vbo != 0) {
            GL15.glDeleteBuffers(vbo);
            vbo = 0;
        }

        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao);
            vao = 0;
        }
    }

    /*
     * Utility used by the meshes.
     */
    protected static float[] createFloatArray(
            java.util.List<Float> values
    ) {

        float[] result =
                new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }

        return result;
    }

    /*
     * Tiny wrapper so the rest of the renderer doesn't
     * need another import.
     */
    private static final class GL11Compat {

        private static void drawArrays(
                int count
        ) {
            org.lwjgl.opengl.GL11.glDrawArrays(
                    org.lwjgl.opengl.GL11.GL_TRIANGLES,
                    0,
                    count
            );
        }
    }
}
