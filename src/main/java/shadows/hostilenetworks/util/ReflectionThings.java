package shadows.hostilenetworks.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.coremod.api.ASMAPI;

public class ReflectionThings {

	private static MethodHandle getExperienceReward;

	static {
		try {
			Method getXPReward = LivingEntity.class.getDeclaredMethod(ASMAPI.mapMethod("func_70693_a"), PlayerEntity.class);
			getXPReward.setAccessible(true);
			getExperienceReward = MethodHandles.lookup().unreflect(getXPReward);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static int getExperienceReward(LivingEntity ent, PlayerEntity player) {
		try {
			return (int) getExperienceReward.invoke(ent, player);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
