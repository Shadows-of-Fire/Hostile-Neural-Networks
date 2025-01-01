package dev.shadowsoffire.hostilenetworks.net;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberContainer;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

public record SetRedstoneStatePayload(int ordinal) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetRedstoneStatePayload> TYPE = new CustomPacketPayload.Type<>(HostileNetworks.loc("redstone_state"));

    public static final StreamCodec<ByteBuf, SetRedstoneStatePayload> CODEC = StreamCodec.composite(ByteBufCodecs.INT, SetRedstoneStatePayload::ordinal, SetRedstoneStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<SetRedstoneStatePayload> {

        @Override
        public Type<SetRedstoneStatePayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, SetRedstoneStatePayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handle(SetRedstoneStatePayload msg, IPayloadContext ctx) {
            if(!(ctx.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (sender.containerMenu instanceof SimChamberContainer simChamber) {
                simChamber.setRedstoneState(msg.ordinal());
            }
        }

        @Override
        public List<ConnectionProtocol> getSupportedProtocols() {
            return List.of(ConnectionProtocol.PLAY);
        }

        @Override
        public Optional<PacketFlow> getFlow() {
            return Optional.of(PacketFlow.SERVERBOUND);
        }

        @Override
        public String getVersion() {
            return "1";
        }
    }


}
