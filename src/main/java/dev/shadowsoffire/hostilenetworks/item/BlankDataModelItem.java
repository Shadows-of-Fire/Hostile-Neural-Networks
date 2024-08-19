package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlankDataModelItem extends Item {

    public BlankDataModelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> list, TooltipFlag pFlag) {
        if (HostileConfig.rightClickToAttune) {
            list.add(Component.translatable("hostilenetworks.info.click_to_attune", Color.withColor("hostilenetworks.color_text.rclick", ChatFormatting.WHITE.getColor()),
                Color.withColor("hostilenetworks.color_text.build", ChatFormatting.GOLD.getColor())).withStyle(ChatFormatting.GRAY));
        }
        else {
            list.add(Component.translatable("hostilenetworks.info.attunment_disabled").withStyle(ChatFormatting.GRAY));
        }
    }

}
