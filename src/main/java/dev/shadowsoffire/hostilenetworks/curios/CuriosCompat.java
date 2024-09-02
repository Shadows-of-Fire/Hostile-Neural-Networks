package dev.shadowsoffire.hostilenetworks.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CuriosCompat {

    public static ItemStack getDeepLearner(Player player) {
        return ItemStack.EMPTY;// return CuriosApi.getCuriosHelper().findFirstCurio(player, Hostile.Items.DEEP_LEARNER.get()).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }
}
