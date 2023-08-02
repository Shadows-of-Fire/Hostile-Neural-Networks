package dev.shadowsoffire.hostilenetworks.item;

import java.util.Comparator;

import javax.annotation.Nullable;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelManager;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MobPredictionItem extends Item implements ITabFiller {

    public static final String TIER = "tier";

    public MobPredictionItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        DataModel model = getStoredModel(pStack);
        Component modelName;
        if (model == null) {
            modelName = Component.literal("BROKEN").withStyle(ChatFormatting.OBFUSCATED);
        }
        else modelName = model.getName();
        return Component.translatable(this.getDescriptionId(pStack), modelName);
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
        DataModelManager.INSTANCE.getValues().stream().sorted(Comparator.comparing(DataModel::getId)).forEach(model -> {
            ItemStack s = new ItemStack(this);
            setStoredModel(s, model);
            output.accept(s);
        });
    }

    @Nullable
    public static DataModel getStoredModel(ItemStack stack) {
        return DataModelItem.getStoredModel(stack);
    }

    public static void setStoredModel(ItemStack stack, DataModel model) {
        DataModelItem.setStoredModel(stack, model);
    }

}
