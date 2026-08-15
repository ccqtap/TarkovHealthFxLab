package com.qq.tarkovhealthfxlab.network;

import com.qq.tarkovhealthfxlab.common.health.InjuryState;
import com.qq.tarkovhealthfxlab.common.health.InjuryStateCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record InjuryStateSyncPacket(UUID playerId, CompoundTag encodedState,
                                    boolean analgesia, boolean repair, boolean regeneration) {
    public static InjuryStateSyncPacket of(UUID playerId, InjuryState state,
                                           boolean analgesia, boolean repair, boolean regeneration) {
        return new InjuryStateSyncPacket(playerId, InjuryStateCodec.encode(state), analgesia, repair, regeneration);
    }

    public static void encode(InjuryStateSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeNbt(packet.encodedState);
        buffer.writeBoolean(packet.analgesia);
        buffer.writeBoolean(packet.repair);
        buffer.writeBoolean(packet.regeneration);
    }

    public static InjuryStateSyncPacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        CompoundTag state = buffer.readNbt();
        return new InjuryStateSyncPacket(playerId, state == null ? new CompoundTag() : state,
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(InjuryStateSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> ClientInjuryState.accept(packet));
        }
        context.setPacketHandled(true);
    }
}
