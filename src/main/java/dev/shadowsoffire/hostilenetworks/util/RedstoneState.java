package dev.shadowsoffire.hostilenetworks.util;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Tri-state redstone control mode shared by HNN machines (Sim Chamber, Loot Fabricator). Determines whether the
 * machine's processing tick is gated on the block's redstone power level.
 */
public enum RedstoneState implements StringRepresentable {

    IGNORED("ignored", ResourceLocation.withDefaultNamespace("textures/item/redstone.png")),
    OFF_WHEN_POWERED("off_when_powered", ResourceLocation.withDefaultNamespace("textures/block/redstone_torch_off.png")),
    ON_WHEN_POWERED("on_when_powered", ResourceLocation.withDefaultNamespace("textures/block/redstone_torch.png"));

    public static final Codec<RedstoneState> CODEC = StringRepresentable.fromEnum(RedstoneState::values);
    public static final StreamCodec<ByteBuf, RedstoneState> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], RedstoneState::ordinal);

    private final String name;
    private final ResourceLocation texture;

    RedstoneState(String name, ResourceLocation texture) {
        this.name = name;
        this.texture = texture;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getKey() {
        return "hostilenetworks.gui.redstone." + this.name;
    }

    public ResourceLocation getResourceLocation() {
        return this.texture;
    }

    /** Whether processing should run given the current redstone power state at the block. */
    public boolean matches(boolean power) {
        return switch (this) {
            case IGNORED -> true;
            case OFF_WHEN_POWERED -> !power;
            case ON_WHEN_POWERED -> power;
        };
    }

    public RedstoneState next() {
        return switch (this) {
            case IGNORED -> OFF_WHEN_POWERED;
            case OFF_WHEN_POWERED -> ON_WHEN_POWERED;
            case ON_WHEN_POWERED -> IGNORED;
        };
    }

}
