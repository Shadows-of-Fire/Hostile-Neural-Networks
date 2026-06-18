package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class TooltipBlockItem extends BlockItem {

    private int lines;

    public TooltipBlockItem(Block block, Properties props, int lines) {
        super(block, props);
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        for (int i = 0; i < lines; i++) {
            String key = this.getDescriptionId() + ".desc" + (i > 0 ? String.valueOf(i + 1) : "");
            list.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
