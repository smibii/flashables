package com.smibii.flashables.client.render.shadow;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ShadowBlockRenderer {
    private ShadowBlockRenderer() {}

    public static void renderBlock(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            Vec3 light,
            Matrix4f view,
            Matrix4f projection
    ) {
        /*
         * This is where the block's six faces need to be emitted
         * into the shadow depth shader.
         *
         * Do NOT call LevelRenderer.renderLevel() here.
         */
    }
}
