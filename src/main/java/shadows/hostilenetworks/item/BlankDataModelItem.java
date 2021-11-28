package shadows.hostilenetworks.item;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import shadows.hostilenetworks.util.Color;

public class BlankDataModelItem extends Item {

	public BlankDataModelItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack pStack, World pLevel, List<ITextComponent> list, ITooltipFlag pFlag) {
		list.add(new TranslationTextComponent("hostilenetworks.info.click_to_attune", Color.withColor("hostilenetworks.color_text.rclick", TextFormatting.WHITE.getColor()), Color.withColor("hostilenetworks.color_text.build", TextFormatting.GOLD.getColor())).withStyle(TextFormatting.GRAY));
	}

}
