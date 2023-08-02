package dev.shadowsoffire.hostilenetworks.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.coremod.api.ASMAPI;

public class ReflectionThings {

    private static MethodHandle getExperienceReward;

    static {
        try {
            Method getXPReward = LivingEntity.class.getDeclaredMethod(ASMAPI.mapMethod("m_213860_"));
            getXPReward.setAccessible(true);
            getExperienceReward = MethodHandles.lookup().unreflect(getXPReward);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getExperienceReward(LivingEntity ent) {
        try {
            return (int) getExperienceReward.invoke(ent);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
