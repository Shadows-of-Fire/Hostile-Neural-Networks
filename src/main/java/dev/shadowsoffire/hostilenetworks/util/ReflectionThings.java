package dev.shadowsoffire.hostilenetworks.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;

public class ReflectionThings {

    private static MethodHandle getBaseExperienceReward;

    static {
        try {
            Method getXPReward = LivingEntity.class.getDeclaredMethod("getBaseExperienceReward");
            getXPReward.setAccessible(true);
            getBaseExperienceReward = MethodHandles.lookup().unreflect(getXPReward);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getBaseExperienceReward(LivingEntity ent) {
        try {
            return (int) getBaseExperienceReward.invoke(ent);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
