package dev.shadowsoffire.hostilenetworks.curios;

import dev.shadowsoffire.hostilenetworks.Hostile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

public class CuriosCompat {

    public static ItemStack getDeepLearner(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Hostile.Items.DEEP_LEARNER.get()).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }
}
