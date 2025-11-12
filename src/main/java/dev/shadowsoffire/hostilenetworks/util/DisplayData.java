package dev.shadowsoffire.hostilenetworks.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.nbt.CompoundTag;

/**
 * @param nbt     NBT data applied to the rendered entity.
 * @param scale   Scale factor applied to the rendered entity. 1 = default scale.
 * @param xOffset X offset applied to the rendered entity.
 * @param yOffset Y offset applied to the rendered entity.
 * @param zOffset Z offset applied to the rendered entity.
 */
public record DisplayData(CompoundTag nbt, float scale, float xOffset, float yOffset, float zOffset) {

    public static final DisplayData DEFAULT = new DisplayData(new CompoundTag(), 1, 0, 0, 0);

    public static final Codec<DisplayData> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(DisplayData::nbt),
            Codec.floatRange(0, 5).optionalFieldOf("scale", 1F).forGetter(DisplayData::scale),
            Codec.floatRange(-5, 5).optionalFieldOf("x_offset", 0F).forGetter(DisplayData::xOffset),
            Codec.floatRange(-5, 5).optionalFieldOf("y_offset", 0F).forGetter(DisplayData::yOffset),
            Codec.floatRange(-5, 5).optionalFieldOf("z_offset", 0F).forGetter(DisplayData::zOffset))
        .apply(inst, DisplayData::new));
}
