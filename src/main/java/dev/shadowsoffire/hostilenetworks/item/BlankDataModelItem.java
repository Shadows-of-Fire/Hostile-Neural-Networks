package dev.shadowsoffire.hostilenetworks.item;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class BlankDataModelItem extends Item {

    public BlankDataModelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
        if (HostileConfig.rightClickToAttune) {
            list.accept(Component.translatable("hostilenetworks.info.click_to_attune", Color.withColor("hostilenetworks.color_text.rclick", ChatFormatting.WHITE.getColor()),
                Color.withColor("hostilenetworks.color_text.build", ChatFormatting.GOLD.getColor())).withStyle(ChatFormatting.GRAY));
        }
        else {
            list.accept(Component.translatable("hostilenetworks.info.attunment_disabled").withStyle(ChatFormatting.GRAY));
        }
    }

}
