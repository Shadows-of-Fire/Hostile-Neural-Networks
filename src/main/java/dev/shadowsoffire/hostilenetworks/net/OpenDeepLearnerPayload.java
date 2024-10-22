package dev.shadowsoffire.hostilenetworks.net;

import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.curios.CuriosCompat;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer.DeepLearnerSource;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenDeepLearnerPayload() implements CustomPacketPayload {

    public static final OpenDeepLearnerPayload INSTANCE = new OpenDeepLearnerPayload();

    public static final Type<OpenDeepLearnerPayload> TYPE = new Type<>(HostileNetworks.loc("open_deep_learner"));

    public static final StreamCodec<ByteBuf, OpenDeepLearnerPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Provider implements PayloadProvider<OpenDeepLearnerPayload> {

        @Override
        public Type<OpenDeepLearnerPayload> getType() {
            return TYPE;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, OpenDeepLearnerPayload> getCodec() {
            return CODEC;
        }

        @Override
        public void handle(OpenDeepLearnerPayload msg, IPayloadContext ctx) {
            Player player = ctx.player();
            if (ModList.get().isLoaded("curios") && player.containerMenu == player.inventoryMenu) {
                ItemStack stack = CuriosCompat.getDeepLearner(player);
                if (stack.is(Hostile.Items.DEEP_LEARNER)) {
                    DeepLearnerSource src = DeepLearnerSource.CURIOS;
                    player.openMenu(new DeepLearnerItem.Provider(src), buf -> buf.writeByte(src.ordinal()));
                }
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
