package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Builds and draws depth-only shadow geometry for every solid block
 * within radius of a light, as a single batched draw using
 * {@link ShadowDepthShader} (this mod's own minimal position/depth
 * shader, not one of Minecraft's built-in ones - its uniform names
 * are guaranteed to match what this class sets, the same way the
 * point/spot light shaders' do).
 * <p>
 * This exists instead of asking Minecraft to render the level from a
 * shadow camera (via {@code LevelRenderer.renderLevel()}) because
 * doing that outside the normal once-per-frame call site is unsafe
 * with Oculus/Iris loaded - see {@link PointLightShadowMap#render}.
 * Rendering only static block geometry ourselves means entities don't
 * cast shadows, which is an accepted trade-off for not being able to
 * safely re-enter the level renderer.
 * <p>
 * {@code view}'s rotation is applied exactly once, here, via the
 * ModelViewMat uniform - {@link ShadowBlockRenderer} writes plain
 * light-relative positions with no transform baked into the vertex
 * data, on purpose (see its class doc).
 */
public final class ShadowGeometryRenderer {
    private ShadowGeometryRenderer() {}

    public static void render(
            ClientLevel level,
            Vec3 light,
            float radius,
            Matrix4f view,
            Matrix4f projection
    ) {
        ShaderInstance shader = ShadowDepthShader.SHADOW_DEPTH;

        if (shader == null) {
            return;
        }

        int minX = (int) Math.floor(light.x - radius);
        int maxX = (int) Math.ceil(light.x + radius);

        int minY = Math.max(level.getMinBuildHeight(), (int) Math.floor(light.y - radius));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, (int) Math.ceil(light.y + radius));

        int minZ = (int) Math.floor(light.z - radius);
        int maxZ = (int) Math.ceil(light.z + radius);

        /*
         * Check for at least one solid block before calling begin() -
         * BufferBuilder expects a matching end()/upload for whatever
         * it's given, and an empty light (floating in open air with
         * nothing in range) is a real case, not just theoretical.
         */
        if (!hasAnySolidBlock(level, minX, maxX, minY, maxY, minZ, maxZ)) {
            return;
        }

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || !state.isSolidRender(level, pos)) {
                        continue;
                    }

                    ShadowBlockRenderer.emitBlock(buffer, level, pos, light);
                }
            }
        }

        RenderSystem.setShader(() -> shader);
        shader.getUniform("ModelViewMat").set(view);
        shader.getUniform("ProjMat").set(projection);

        /*
         * See ShadowBlockRenderer.quad(): winding isn't tracked per
         * face, so culling has to stay off for this draw regardless
         * of whatever the caller had it set to.
         */
        RenderSystem.disableCull();

        BufferUploader.drawWithShader(buffer.end());
    }

    private static boolean hasAnySolidBlock(
            ClientLevel level,
            int minX, int maxX,
            int minY, int maxY,
            int minZ, int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (!state.isAir() && state.isSolidRender(level, pos)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
