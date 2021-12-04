package shadows.hostilenetworks.item;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;

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
	public void fillItemCategory(ItemGroup pGroup, NonNullList<ItemStack> pItems) {
		if (this.allowdedIn(pGroup)) {
			for (DataModel model : DataModelManager.INSTANCE.getAllModels()) {
				ItemStack s = new ItemStack(this);
				setStoredModel(s, model);
				pItems.add(s);
			}
		}
	}

	@Nullable
	public static DataModel getStoredModel(ItemStack stack) {
		return DataModelItem.getStoredModel(stack);
	}

	public static void setStoredModel(ItemStack stack, DataModel model) {
		DataModelItem.setStoredModel(stack, model);
	}

}