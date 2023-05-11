package shadows.hostilenetworks.util;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ClientEntityCache {

	private static final Map<EntityType<?>, LivingEntity> CACHE = new WeakHashMap<>();

	@SuppressWarnings("unchecked")
	public static <T extends LivingEntity> T computeIfAbsent(EntityType<T> type, Level level, CompoundTag displayNbt) {
		return (T) CACHE.computeIfAbsent(type, k -> {
			T t = type.create(level);
			t.load(displayNbt);
			return t;
		});
	}

	public static void tick() {
		CACHE.values().forEach(e -> {
			e.tickCount++;
		});
	}
}
