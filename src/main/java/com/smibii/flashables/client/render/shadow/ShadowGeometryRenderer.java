package com.smibii.flashables.client.render.shadow;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ShadowGeometryRenderer {
    private ShadowGeometryRenderer() {}

    public static void render(
            ClientLevel level,
            Vec3 light,
            float radius,
            Matrix4f view,
            Matrix4f projection
    ) {
        int minX = (int) Math.floor(light.x - radius);
        int maxX = (int) Math.ceil(light.x + radius);

        int minY = Math.max(
                level.getMinBuildHeight(),
                (int) Math.floor(light.y - radius)
        );

        int maxY = Math.min(
                level.getMaxBuildHeight() - 1,
                (int) Math.ceil(light.y + radius)
        );

        int minZ = (int) Math.floor(light.z - radius);
        int maxZ = (int) Math.ceil(light.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }

                    if (!state.isSolidRender(level, pos)) {
                        continue;
                    }

                    ShadowBlockRenderer.renderBlock(
                            level,
                            pos,
                            state,
                            light,
                            view,
                            projection
                    );
                }
            }
        }
    }
}
