package shadows.hostilenetworks.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;
import shadows.hostilenetworks.client.DataModelItemStackRenderer;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.util.Color;
import shadows.placebo.util.ClientUtil;

public class DataModelItem extends Item {

	public static final String DATA_MODEL = "data_model";
	public static final String ID = "id";
	public static final String DATA = "data";
	public static final String ITERATIONS = "iterations";

	public DataModelItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> list, TooltipFlag pFlag) {
		if (ClientUtil.isHoldingShift()) {
			DataModel model = getStoredModel(pStack);
			if (model == null) {
				list.add(new TranslatableComponent("Error: %s", new TextComponent("Broke_AF").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.GRAY)));
				return;
			}
			int data = getData(pStack);
			ModelTier tier = ModelTier.getByData(data);
			list.add(new TranslatableComponent("hostilenetworks.info.tier", tier.getComponent()));
			int dProg = data - tier.data;
			int dMax = tier.next().data - tier.data;
			if (tier != ModelTier.SELF_AWARE) {
				list.add(new TranslatableComponent("hostilenetworks.info.data", new TranslatableComponent("hostilenetworks.info.dprog", dProg, dMax).withStyle(ChatFormatting.GRAY)));
				list.add(new TranslatableComponent("hostilenetworks.info.dpk", new TextComponent("" + tier.dataPerKill).withStyle(ChatFormatting.GRAY)));
			}
			list.add(new TranslatableComponent("hostilenetworks.info.sim_cost", new TranslatableComponent("hostilenetworks.info.rft", model.getSimCost()).withStyle(ChatFormatting.GRAY)));
		} else {
			list.add(new TranslatableComponent("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", ChatFormatting.WHITE.getColor())).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void fillItemCategory(CreativeModeTab pGroup, NonNullList<ItemStack> pItems) {
		if (this.allowdedIn(pGroup)) {
			for (DataModel model : DataModelManager.INSTANCE.getAllModels()) {
				ItemStack s = new ItemStack(this);
				setStoredModel(s, model);
				pItems.add(s);
			}
		}
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
	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(new IItemRenderProperties() {
			DataModelItemStackRenderer dmisr = new DataModelItemStackRenderer();

			@Override
			public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
				return dmisr;
			}
		});
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

	public static int getData(ItemStack stack) {
		return stack.getOrCreateTagElement(DATA_MODEL).getInt(DATA);
	}

	public static void setData(ItemStack stack, int data) {
		stack.getOrCreateTagElement(DATA_MODEL).putInt(DATA, data);
	}

	public static int getIters(ItemStack stack) {
		return stack.getOrCreateTagElement(DATA_MODEL).getInt(ITERATIONS);
	}

	public static void setIters(ItemStack stack, int data) {
		stack.getOrCreateTagElement(DATA_MODEL).putInt(ITERATIONS, data);
	}

	public static boolean matchesInput(ItemStack model, ItemStack stack) {
		DataModel dModel = getStoredModel(model);
		if (dModel == null) return false;
		ItemStack input = dModel.getInput();
		boolean item = input.getItem() == stack.getItem();
		if (input.hasTag()) {
			if (stack.hasTag()) {
				CompoundTag t1 = input.getTag();
				CompoundTag t2 = stack.getTag();
				for (String s : t1.getAllKeys()) {
					if (!t1.get(s).equals(t2.get(s))) return false;
				}
				return true;
			} else return false;
		} else return item;
	}

}
