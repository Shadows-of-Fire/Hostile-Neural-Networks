package shadows.hostilenetworks.item;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;

public class MobPredictionItem extends Item {

	public static final String TIER = "tier";

	public MobPredictionItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public Component getName(ItemStack pStack) {
		DataModel model = getStoredModel(pStack);
		Component modelName;
		if (model == null) {
			modelName = new TextComponent("BROKEN").withStyle(ChatFormatting.OBFUSCATED);
		} else modelName = model.getName();
		return new TranslatableComponent(this.getDescriptionId(pStack), modelName);
	}

	@Override
	public void fillItemCategory(CreativeModeTab pGroup, NonNullList<ItemStack> pItems) {
		if (this.allowdedIn(pGroup)) {
			for (DataModel model : DataModelManager.INSTANCE.getValues()) {
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