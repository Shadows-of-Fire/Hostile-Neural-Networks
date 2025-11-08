package dev.shadowsoffire.hostilenetworks.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;

public record DisplayEntity(EntityType<?> type, CompoundTag nbt, float scale, float xOffset, float yOffset, float zOffset) {

    public DisplayEntity(EntityType<?> type, CompoundTag nbt) {
        this(type, nbt, 1.0f, 0.0f, 0.0f, 0.0f);
    }

}
