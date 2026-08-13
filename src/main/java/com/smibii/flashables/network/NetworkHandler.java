package com.smibii.flashables.network;

import com.smibii.flashables.Flashables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Flashables.location("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {}

    public static <T> void broadcast(T packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
    public static <T> void broadcastToAllTracking(T packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }
    public static <T> void broadcastTo(T packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <T> void broadcastToServer(T packet) {
        INSTANCE.sendToServer(packet);
    }
}
