package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Emits one solid block's exposed faces as depth-only triangles into
 * a shared buffer, positioned relative to the light rather than the
 * camera - {@link ShadowGeometryRenderer} batches many of these into
 * a single draw per shadow map.
 * <p>
 * Vertices are written as plain light-relative local positions, with
 * no rotation applied here: {@link ShadowGeometryRenderer} applies
 * the light's view rotation once, in the vertex shader, via the
 * ModelViewMat uniform. Baking that same rotation into the vertex
 * data here too (e.g. via {@code buffer.vertex(Matrix4f, x, y, z)},
 * which transforms the position immediately) would apply it twice,
 * scattering every block to the wrong place in the shadow map.
 * <p>
 * Blocks are rendered as their full unit-cube bounding box rather
 * than their actual shape, so stairs/slabs/fences/etc. over-occlude
 * slightly. That's a deliberate simplification: a precise version
 * would need each block's real {@code VoxelShape} faces, which is a
 * lot more work for a shadow map that's already only an approximation
 * (single dynamic light, not baked).
 */
public final class ShadowBlockRenderer {
    private ShadowBlockRenderer() {}

    public static void emitBlock(
            BufferBuilder buffer,
            ClientLevel level,
            BlockPos pos,
            Vec3 light
    ) {
        float x0 = (float) (pos.getX() - light.x);
        float y0 = (float) (pos.getY() - light.y);
        float z0 = (float) (pos.getZ() - light.z);
        float x1 = x0 + 1.0f;
        float y1 = y0 + 1.0f;
        float z1 = z0 + 1.0f;

        /*
         * Skip faces touching another solid block - they can never be
         * seen by the shadow camera, so there's no point rasterizing
         * them. For a solid cluster of blocks (the inside of a
         * mountain, say) this is the difference between emitting
         * every block's six faces and emitting almost nothing.
         */

        if (!isSolidNeighbor(level, pos, Direction.WEST)) {
            quad(buffer, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        }

        if (!isSolidNeighbor(level, pos, Direction.EAST)) {
            quad(buffer, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0);
        }

        if (!isSolidNeighbor(level, pos, Direction.DOWN)) {
            quad(buffer, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
        }

        if (!isSolidNeighbor(level, pos, Direction.UP)) {
            quad(buffer, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1);
        }

        if (!isSolidNeighbor(level, pos, Direction.NORTH)) {
            quad(buffer, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0);
        }

        if (!isSolidNeighbor(level, pos, Direction.SOUTH)) {
            quad(buffer, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
        }
    }

    private static boolean isSolidNeighbor(ClientLevel level, BlockPos pos, Direction direction) {
        BlockPos neighbor = pos.relative(direction);
        BlockState state = level.getBlockState(neighbor);
        return state.isSolidRender(level, neighbor);
    }

    /*
     * Winding order doesn't matter here - ShadowGeometryRenderer
     * disables face culling for this draw, since getting the winding
     * wrong per-face would silently drop geometry from the shadow map
     * (culled backfaces just never rasterize, no error), which is a
     * much worse failure mode than the minor overdraw cost of drawing
     * both sides.
     */
    private static void quad(
            BufferBuilder buffer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3
    ) {
        vertex(buffer, x0, y0, z0);
        vertex(buffer, x1, y1, z1);
        vertex(buffer, x2, y2, z2);

        vertex(buffer, x0, y0, z0);
        vertex(buffer, x2, y2, z2);
        vertex(buffer, x3, y3, z3);
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float z) {
        buffer.vertex(x, y, z).endVertex();
    }
}
