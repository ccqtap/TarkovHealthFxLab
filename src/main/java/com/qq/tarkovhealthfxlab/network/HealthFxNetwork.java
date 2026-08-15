package com.qq.tarkovhealthfxlab.network;

import com.qq.tarkovhealthfxlab.TarkovHealthFxLab;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class HealthFxNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(TarkovHealthFxLab.MODID, "health_state"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static boolean initialized;

    private HealthFxNetwork() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        CHANNEL.registerMessage(0, InjuryStateSyncPacket.class, InjuryStateSyncPacket::encode,
                InjuryStateSyncPacket::decode, InjuryStateSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        initialized = true;
    }

    public static void send(ServerPlayer player, InjuryStateSyncPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
