package shadows.hostilenetworks.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.util.Color;
import shadows.placebo.util.ClientUtil;

public class MobPredictionItem extends Item {

	public static final String TIER = "tier";

	public MobPredictionItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public ITextComponent getName(ItemStack pStack) {
		DataModel model = getStoredModel(pStack);
		ITextComponent modelName;
		if (model == null) {
			modelName = new StringTextComponent("BROKEN").withStyle(TextFormatting.OBFUSCATED);
		} else modelName = model.getName();
		return new TranslationTextComponent(this.getDescriptionId(pStack), modelName);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack pStack, World pLevel, List<ITextComponent> list, ITooltipFlag pFlag) {
		if (ClientUtil.isHoldingShift()) {
			DataModel model = getStoredModel(pStack);
			if (model == null) {
				list.add(new TranslationTextComponent("Error: %s", new StringTextComponent("Broke_AF").withStyle(TextFormatting.OBFUSCATED, TextFormatting.GRAY)));
				return;
			}
			ModelTier tier = getTier(pStack);
			list.add(new TranslationTextComponent("hostilenetworks.info.tier", tier.getComponent()));
			list.add(new TranslationTextComponent("hostilenetworks.info.looting", new TranslationTextComponent("enchantment.level." + (tier.ordinal() - 1)).withStyle(TextFormatting.GRAY)));
			list.add(new TranslationTextComponent("hostilenetworks.info.player", trueFalse(tier.ordinal() >= ModelTier.ADVANCED.ordinal())));

			list.add(new TranslationTextComponent("hostilenetworks.info.rolls", new StringTextComponent("" + HostileConfig.rollsArray[tier.ordinal()]).withStyle(TextFormatting.GRAY)));

			list.add(new TranslationTextComponent("hostilenetworks.info.exp", trueFalse(tier.ordinal() >= ModelTier.SUPERIOR.ordinal())));
		} else {
			list.add(new TranslationTextComponent("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", TextFormatting.WHITE.getColor())).withStyle(TextFormatting.GRAY));
		}
	}

	@Override
	public void fillItemCategory(ItemGroup pGroup, NonNullList<ItemStack> pItems) {
		if (this.allowdedIn(pGroup)) {
			for (DataModel model : DataModelManager.INSTANCE.getAllModels()) {
				ItemStack s = new ItemStack(this);
				setStoredModel(s, model);
				setTier(s, ModelTier.BASIC);
				pItems.add(s);
			}
		}
	}

	private static ITextComponent trueFalse(boolean flag) {
		TranslationTextComponent msg = new TranslationTextComponent("hostilenetworks.color_text." + (flag ? "true" : "false"));
		return msg.withStyle(flag ? TextFormatting.GOLD : TextFormatting.RED);
	}

	public static void setTier(ItemStack stack, ModelTier tier) {
		stack.getOrCreateTag().putInt(TIER, tier.ordinal());
	}

	public static ModelTier getTier(ItemStack stack) {
		return ModelTier.values()[stack.getOrCreateTag().getInt(TIER)];
	}

	@Nullable
	public static DataModel getStoredModel(ItemStack stack) {
		return DataModelItem.getStoredModel(stack);
	}

	public static void setStoredModel(ItemStack stack, DataModel model) {
		DataModelItem.setStoredModel(stack, model);
	}

}
