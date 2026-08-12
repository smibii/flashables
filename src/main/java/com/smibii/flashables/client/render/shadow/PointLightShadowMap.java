package com.smibii.flashables.client.render.shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.smibii.flashables.client.render.PointLightRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.util.List;

public class PointLightShadowMap {
    /*
     * Cubemap face resolution. Memory scales with the
     * square of this value, times 6 faces, times the
     * depth format's byte size below - so this is by
     * far the biggest lever for VRAM usage. 512 is
     * plenty for a shadow that's only ever seen at the
     * scale of a handheld light's radius; bump it back
     * up only if you can actually see banding/aliasing
     * on the shadow edges.
     */
    public static final int SIZE = 512;

    private static final float NEAR_PLANE = 0.05f;

    private static int framebuffer = -1;
    private static int depthCubemap = -1;

    private static boolean initialized = false;

    private static final RandomSource RANDOM = RandomSource.create();

    private PointLightShadowMap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(PointLightShadowMap::init);
            return;
        }

        depthCubemap = GL11.glGenTextures();

        GL11.glBindTexture(
                GL32.GL_TEXTURE_CUBE_MAP,
                depthCubemap
        );

        for (int face = 0; face < 6; face++) {
            /*
             * 16-bit depth instead of 24-bit: half the
             * memory per texel. The shadow's near/far
             * range is just [0.05, lightRadius], which
             * is small enough that 16 bits of precision
             * is not a visible difference here, and the
             * shader's ShadowBias already covers the
             * extra rounding error.
             */
            GL11.glTexImage2D(
                    GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                    0,
                    GL14.GL_DEPTH_COMPONENT16,
                    SIZE,
                    SIZE,
                    0,
                    GL11.GL_DEPTH_COMPONENT,
                    GL11.GL_FLOAT,
                    0
            );
        }

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_MIN_FILTER,
                GL11.GL_NEAREST
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_MAG_FILTER,
                GL11.GL_NEAREST
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_WRAP_S,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL11.GL_TEXTURE_WRAP_T,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glTexParameteri(
                GL32.GL_TEXTURE_CUBE_MAP,
                GL12.GL_TEXTURE_WRAP_R,
                GL12.GL_CLAMP_TO_EDGE
        );

        GL11.glBindTexture(
                GL32.GL_TEXTURE_CUBE_MAP,
                0
        );

        framebuffer = GL30.glGenFramebuffers();

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                framebuffer
        );

        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                depthCubemap,
                0
        );

        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        int status = GL30.glCheckFramebufferStatus(
                GL30.GL_FRAMEBUFFER
        );

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GL30.glBindFramebuffer(
                    GL30.GL_FRAMEBUFFER,
                    0
            );

            throw new IllegalStateException(
                    "Point light shadow framebuffer incomplete: " + status
            );
        }

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                0
        );

        initialized = true;
    }

    public static void render(float partialTick) {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        if (!initialized) {
            init();
        }

        Vec3 light = PointLightRenderer.LIGHT.getPosition();

        float radius = PointLightRenderer.LIGHT.getRadius();

        /*
         * Gathered once here rather than inside
         * renderFace() - a world query is the same
         * result for all six faces, so doing it six
         * times per frame would just be wasted CPU
         * time for no extra shadow accuracy.
         */
        AABB bounds = new AABB(
                light.x - radius,
                light.y - radius,
                light.z - radius,
                light.x + radius,
                light.y + radius,
                light.z + radius
        );

        List<Entity> entities = level.getEntities(
                (Entity) null,
                bounds,
                entity -> shouldCastShadow(entity, light)
        );

        int oldFramebuffer = GL11.glGetInteger(
                GL30.GL_FRAMEBUFFER_BINDING
        );

        int[] viewport = new int[4];

        GL11.glGetIntegerv(
                GL11.GL_VIEWPORT,
                viewport
        );

        boolean depthTest = GL11.glIsEnabled(
                GL11.GL_DEPTH_TEST
        );

        boolean cull = GL11.glIsEnabled(
                GL11.GL_CULL_FACE
        );

        boolean blend = GL11.glIsEnabled(
                GL11.GL_BLEND
        );

        /*
         * The six faces below each overwrite
         * RenderSystem's projection matrix with a
         * 90 degree shadow-face projection. That is
         * global state, not something scoped to our
         * framebuffer, so it must be saved here and
         * restored once all faces are done or every
         * draw call for the rest of the frame will
         * keep using the shadow projection.
         */
        Matrix4f savedProjectionMatrix = new Matrix4f(
                RenderSystem.getProjectionMatrix()
        );

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                framebuffer
        );

        GL11.glViewport(
                0,
                0,
                SIZE,
                SIZE
        );

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        GL11.glColorMask(
                false,
                false,
                false,
                false
        );

        /*
         * Render all six directions.
         */
        renderFace(
                level,
                entities,
                light,
                radius,
                0,
                partialTick
        );

        renderFace(
                level,
                entities,
                light,
                radius,
                1,
                partialTick
        );

        renderFace(
                level,
                entities,
                light,
                radius,
                2,
                partialTick
        );

        renderFace(
                level,
                entities,
                light,
                radius,
                3,
                partialTick
        );

        renderFace(
                level,
                entities,
                light,
                radius,
                4,
                partialTick
        );

        renderFace(
                level,
                entities,
                light,
                radius,
                5,
                partialTick
        );

        /*
         * Restore state.
         */
        RenderSystem.setProjectionMatrix(
                savedProjectionMatrix,
                VertexSorting.DISTANCE_TO_ORIGIN
        );

        GL30.glBindFramebuffer(
                GL30.GL_FRAMEBUFFER,
                oldFramebuffer
        );

        GL11.glViewport(
                viewport[0],
                viewport[1],
                viewport[2],
                viewport[3]
        );

        GL11.glColorMask(
                true,
                true,
                true,
                true
        );

        if (depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        if (cull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (blend) {
            RenderSystem.enableBlend();
        } else {
            RenderSystem.disableBlend();
        }

        RenderSystem.depthMask(true);
    }

    private static void renderFace(
            Level level,
            List<Entity> entities,
            Vec3 light,
            float radius,
            int face,
            float partialTick
    ) {
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                depthCubemap,
                0
        );

        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        GL11.glClear(
                GL11.GL_DEPTH_BUFFER_BIT
        );

        /*
         * Create the 90 degree camera projection.
         */
        Matrix4f projection = new Matrix4f()
                .perspective(
                        (float) Math.toRadians(90.0),
                        1.0f,
                        NEAR_PLANE,
                        radius
                );

        RenderSystem.setProjectionMatrix(
                projection,
                VertexSorting.DISTANCE_TO_ORIGIN
        );

        /*
         * Camera direction for this cubemap face.
         */
        Matrix4f view = createViewMatrix(face);

        RenderSystem.getModelViewStack().pushPose();

        RenderSystem.getModelViewStack()
                .last()
                .pose()
                .set(view);

        RenderSystem.applyModelViewMatrix();

        /*
         * Blocks and entities share one PoseStack and
         * one buffer source so both flush together in
         * a single endBatch() call below, instead of
         * each maintaining and flushing its own.
         */
        PoseStack poseStack = new PoseStack();

        MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance()
                        .renderBuffers()
                        .bufferSource();

        renderBlocks(
                level,
                light,
                radius,
                poseStack,
                bufferSource
        );

        renderEntities(
                entities,
                light,
                partialTick,
                poseStack,
                bufferSource
        );

        bufferSource.endBatch();

        RenderSystem.getModelViewStack().popPose();

        RenderSystem.applyModelViewMatrix();
    }

    private static Matrix4f createViewMatrix(int face) {
        Matrix4f matrix = new Matrix4f();

        switch (face) {
            /*
             * +X
             */
            case 0 -> matrix
                    .rotateY((float) Math.toRadians(90.0));

            /*
             * -X
             */
            case 1 -> matrix
                    .rotateY((float) Math.toRadians(-90.0));

            /*
             * +Y
             */
            case 2 -> matrix
                    .rotateX((float) Math.toRadians(-90.0));

            /*
             * -Y
             */
            case 3 -> matrix
                    .rotateX((float) Math.toRadians(90.0));

            /*
             * +Z
             */
            case 4 -> matrix
                    .rotateY((float) Math.toRadians(180.0));

            /*
             * -Z
             *
             * No rotation needed - the camera's default
             * forward direction (-Z) already matches
             * this face, so identity is correct here.
             */
            case 5 -> {
            }

            default -> {
            }
        }

        return matrix;
    }

    private static void renderBlocks(
            Level level,
            Vec3 light,
            float radius,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        BlockRenderDispatcher blockRenderer =
                minecraft.getBlockRenderer();

        int minX = (int) Math.floor(light.x - radius);
        int maxX = (int) Math.ceil(light.x + radius);

        int minY = Math.max(
                level.getMinBuildHeight(),
                (int) Math.floor(light.y - radius)
        );

        int maxY = Math.min(
                level.getMaxBuildHeight(),
                (int) Math.ceil(light.y + radius)
        );

        int minZ = (int) Math.floor(light.z - radius);
        int maxZ = (int) Math.ceil(light.z + radius);

        BlockPos.MutableBlockPos pos =
                new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    double dx = x + 0.5 - light.x;
                    double dy = y + 0.5 - light.y;
                    double dz = z + 0.5 - light.z;

                    if (dx * dx + dy * dy + dz * dz >
                            radius * radius) {
                        continue;
                    }

                    pos.set(
                            x,
                            y,
                            z
                    );

                    BlockState state =
                            level.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }

                    if (!state.isSolidRender(
                            level,
                            pos
                    )) {
                        continue;
                    }

                    /*
                     * Translate world coordinates relative
                     * to the point light.
                     */
                    poseStack.pushPose();

                    poseStack.translate(
                            x - light.x,
                            y - light.y,
                            z - light.z
                    );

                    RANDOM.setSeed(
                            BlockPos.asLong(
                                    x,
                                    y,
                                    z
                            )
                    );

                    blockRenderer.renderSingleBlock(
                            state,
                            poseStack,
                            bufferSource,
                            15728880,
                            OverlayTexture.NO_OVERLAY
                    );

                    poseStack.popPose();
                }
            }
        }
    }

    /*
     * Any entity this close to the light is almost
     * certainly the thing carrying/generating the
     * light itself (or standing right on top of it).
     * At that range every limb sits nearly edge-on to
     * the light, so its silhouette projects as long,
     * thin, near-degenerate wedges - the light is
     * basically grazing along the model instead of
     * lighting it face-on. Excluding anything this
     * close avoids that self-shadow starburst without
     * needing to know which specific entity owns the
     * light.
     */
    private static final double MIN_ENTITY_SHADOW_DISTANCE_SQ = 1.5 * 1.5;

    private static boolean shouldCastShadow(Entity entity, Vec3 light) {
        /*
         * Invisible entities (potions, vanished
         * players, etc.) shouldn't leave a shadow
         * behind them, and anything already dead/
         * removed this tick has no business being
         * drawn at all.
         */
        if (!entity.isAlive() || entity.isInvisible()) {
            return false;
        }

        return entity.distanceToSqr(
                light.x,
                light.y,
                light.z
        ) > MIN_ENTITY_SHADOW_DISTANCE_SQ;
    }

    private static void renderEntities(
            List<Entity> entities,
            Vec3 light,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource
    ) {
        if (entities.isEmpty()) {
            return;
        }

        EntityRenderDispatcher entityRenderer =
                Minecraft.getInstance()
                        .getEntityRenderDispatcher();

        for (Entity entity : entities) {
            double entityX = Mth.lerp(
                    partialTick,
                    entity.xOld,
                    entity.getX()
            );

            double entityY = Mth.lerp(
                    partialTick,
                    entity.yOld,
                    entity.getY()
            );

            double entityZ = Mth.lerp(
                    partialTick,
                    entity.zOld,
                    entity.getZ()
            );

            float yaw = Mth.lerp(
                    partialTick,
                    entity.yRotO,
                    entity.getYRot()
            );

            /*
             * Same trick as the blocks: the shadow
             * "camera" sits at the light with a
             * rotation-only view matrix, so entities
             * need to be positioned relative to the
             * light rather than relative to the
             * player camera.
             */
            entityRenderer.render(
                    entity,
                    entityX - light.x,
                    entityY - light.y,
                    entityZ - light.z,
                    yaw,
                    partialTick,
                    poseStack,
                    bufferSource,
                    15728880
            );
        }
    }

    public static int getTexture() {
        return depthCubemap;
    }

    public static int getFramebuffer() {
        return framebuffer;
    }

    public static int getSize() {
        return SIZE;
    }

    public static float getNearPlane() {
        return NEAR_PLANE;
    }

    public static void destroy() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(
                    PointLightShadowMap::destroy
            );
            return;
        }

        if (depthCubemap != -1) {
            GL11.glDeleteTextures(
                    depthCubemap
            );

            depthCubemap = -1;
        }

        if (framebuffer != -1) {
            GL30.glDeleteFramebuffers(
                    framebuffer
            );

            framebuffer = -1;
        }

        initialized = false;
    }
}