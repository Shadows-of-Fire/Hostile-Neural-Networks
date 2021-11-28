package shadows.hostilenetworks.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.util.Color;
import shadows.placebo.util.ClientUtil;

public class DataModelItem extends Item {

	public static final String DATA_MODEL = "data_model";
	public static final String ID = "id";
	public static final String KILLS = "kills";

	public DataModelItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void appendHoverText(ItemStack pStack, World pLevel, List<ITextComponent> list, ITooltipFlag pFlag) {
		if (ClientUtil.isHoldingShift()) {

		} else {
			list.add(new TranslationTextComponent("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", TextFormatting.WHITE.getColor())).withStyle(TextFormatting.GRAY));
		}
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

	/**
	 * Retrieves the data model from a data model itemstack.
	 * @return The contained data model.  Realisitcally should never be null.
	 */
	@Nullable
	public static DataModel getStoredModel(ItemStack stack) {
		if (!stack.hasTag()) return null;
		String dmKey = stack.getOrCreateTagElement(DATA_MODEL).getString(ID);
		return DataModelManager.INSTANCE.getModel(new ResourceLocation(dmKey));
	}

	public static void setStoredModel(ItemStack stack, DataModel model) {
		stack.removeTagKey(DATA_MODEL);
		stack.getOrCreateTagElement(DATA_MODEL).putString(ID, model.getId().toString());
	}

	public static int getKills(ItemStack stack) {
		return stack.getOrCreateTagElement(DATA_MODEL).getInt(KILLS);
	}

}
