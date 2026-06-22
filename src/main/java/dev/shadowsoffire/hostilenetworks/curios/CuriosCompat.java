package dev.shadowsoffire.hostilenetworks.curios;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileEvents;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public class CuriosCompat {

    public static ItemStack getDeepLearner(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inv -> inv.findFirstCurio(Hostile.Items.DEEP_LEARNER.value()).map(SlotResult::stack).orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
    }

    public static void tryUpdateDeepLearner(Player player, EntityType<?> type, int bonus) {
        applyToEquippedDeepLearner(player, stack -> HostileEvents.updateModels(stack, type, bonus));
    }

    public static void tryUpdateDeepLearner(Player player, Block block, int bonus, LootContext ctx) {
        applyToEquippedDeepLearner(player, stack -> HostileEvents.updateModels(stack, block, bonus, ctx));
    }

    private static void applyToEquippedDeepLearner(Player player, Consumer<ItemStack> mutator) {
        ICuriosItemHandler inv = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inv != null) {
            SlotResult result = inv.findFirstCurio(Hostile.Items.DEEP_LEARNER.value()).orElse(null);
            if (result != null) {
                ItemStack stack = result.stack();
                if (stack.getItem() instanceof DeepLearnerItem) {
                    mutator.accept(stack);
                    inv.setEquippedCurio(result.slotContext().identifier(), result.slotContext().index(), stack);
                }
            }
        }
    }
}
