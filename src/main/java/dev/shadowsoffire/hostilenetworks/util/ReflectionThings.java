package dev.shadowsoffire.hostilenetworks.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Reflective accessors for protected entity methods.
 * <p>
 * These cannot be opened with an access transformer, since widening the access of a method that subclasses override
 * breaks the recompilation of those subclasses.
 */
public class ReflectionThings {

    private static final MethodHandle GET_BASE_EXPERIENCE_REWARD = findVirtual("getBaseExperienceReward", ServerLevel.class);

    /**
     * Returns the base experience reward of an entity.
     * <p>
     * In 26.1 {@code LivingEntity#getBaseExperienceReward} takes the {@link ServerLevel}. Most implementations ignore it,
     * but it is unavailable on the client (where the Deep Learner GUI computes statistics), so implementations that do
     * use it are caught and reported as 0.
     */
    public static int getBaseExperienceReward(LivingEntity ent) {
        try {
            return (int) GET_BASE_EXPERIENCE_REWARD.invoke(ent, ent.level() instanceof ServerLevel sl ? sl : null);
        }
        catch (Throwable e) {
            return 0;
        }
    }

    private static MethodHandle findVirtual(String name, Class<?>... params) {
        try {
            Method method = LivingEntity.class.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to resolve LivingEntity#" + name, e);
        }
    }

}
