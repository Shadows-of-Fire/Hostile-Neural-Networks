package dev.shadowsoffire.hostilenetworks.util;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class MiscCodecs {

    public static final Codec<List<EntityType<?>>> OPTIONAL_ENTITY_TYPE_LIST = Identifier.CODEC.listOf().xmap(
        list -> list.stream().map(BuiltInRegistries.ENTITY_TYPE::getOptional).filter(Optional::isPresent).<EntityType<?>>map(Optional::get).toList(),
        list -> list.stream().map(BuiltInRegistries.ENTITY_TYPE::getKey).toList());

}
