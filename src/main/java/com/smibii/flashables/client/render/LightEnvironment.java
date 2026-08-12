package com.smibii.flashables.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Boosts a light's apparent brightness when it's the dominant light
 * source nearby (deep caves, night time) versus when it's competing
 * with strong ambient/sky light (bright daylight outdoors), so lights
 * don't look washed out in the sun or invisible underground.
 */
public final class LightEnvironment {
    private LightEnvironment() {}

    public static float getMultiplier(Level level, Vec3 position, float radius) {
        BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);

        float environmentMultiplier = getEasedTimeFactor(level.dayTime());

        int blockLight = level.getBrightness(LightLayer.BLOCK, blockPos);
        int skyLight = level.getBrightness(LightLayer.SKY, blockPos);

        boolean dark =
                !hasNearbySkyLight(level, blockPos, (int) radius) &&
                        !isSkyExposed(level, blockPos, (int) radius) &&
                        blockLight <= 4 &&
                        skyLight <= 4;

        return dark ? 5.0f : environmentMultiplier;
    }

    private static float getEasedTimeFactor(long worldTime) {
        long timeOfDay = worldTime % 24000;
        float progress;

        if (timeOfDay >= 12000 && timeOfDay <= 13000) {
            progress = (timeOfDay - 12000) / 1000.0f;
        } else if (timeOfDay > 13000 && timeOfDay < 23000) {
            progress = 1.0f;
        } else if (timeOfDay >= 21000 && timeOfDay <= 24000) {
            progress = (24000 - timeOfDay) / 3000.0f;
        } else {
            progress = 0.0f;
        }

        return 1.0f + (progress * 4.0f);
    }

    private static boolean isSkyExposed(Level level, BlockPos pos, int maxDistance) {
        for (int y = pos.getY() + 1; y <= pos.getY() + maxDistance && y < level.getMaxBuildHeight(); y++) {
            BlockPos above = new BlockPos(pos.getX(), y, pos.getZ());

            if (!level.getBlockState(above).getBlock().defaultBlockState().isAir()) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasNearbySkyLight(Level level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos check = pos.offset(x, 0, z);

                int skyLight = level.getBrightness(LightLayer.SKY, check);

                if (skyLight >= 12) {
                    return true;
                }
            }
        }

        return false;
    }
}