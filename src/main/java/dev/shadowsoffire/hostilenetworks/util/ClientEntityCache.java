package dev.shadowsoffire.hostilenetworks.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Caches and retains client entities for rendering.
 * <p>
 * Entities are cleared on level unload.
 */
@EventBusSubscriber(bus = Bus.FORGE, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class ClientEntityCache {

    private static final Map<Level, Map<EntityType<?>, LivingEntity>> CACHE = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> T computeIfAbsent(EntityType<T> type, Level level, CompoundTag displayNbt) {
        var map = CACHE.computeIfAbsent(level, l -> new IdentityHashMap<>());
        return (T) map.computeIfAbsent(type, k -> {
            T t = type.create(level);
            t.load(displayNbt);
            return t;
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            CACHE.values().stream().map(Map::values).flatMap(Collection::stream).forEach(e -> {
                e.tickCount++;
            });
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            CACHE.remove(event.getLevel());
        }
    }

}
