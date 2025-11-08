package dev.shadowsoffire.hostilenetworks.util;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Caches and retains client entities for rendering.
 * <p>
 * Entities are cleared on level unload.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class ClientEntityCache {

    private static final Map<Level, Map<EntityType<?>, Entity>> CACHE = new IdentityHashMap<>();

    public static Entity computeIfAbsent(DisplayEntity display, Level level) {
        var map = CACHE.computeIfAbsent(level, l -> new IdentityHashMap<>());
        return map.computeIfAbsent(display.type(), k -> {
            Entity ent = k.create(level);
            ent.load(display.nbt());
            return ent;
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        CACHE.values().stream().map(Map::values).flatMap(Collection::stream).forEach(e -> {
            e.tickCount++;
        });
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            CACHE.remove(event.getLevel());
        }
    }

}
