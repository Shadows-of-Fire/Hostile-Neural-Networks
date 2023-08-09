package dev.shadowsoffire.hostilenetworks.item;

import java.util.Comparator;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
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
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(pStack);
        Component modelName;
        if (!model.isBound()) {
            modelName = Component.literal("BROKEN").withStyle(ChatFormatting.OBFUSCATED);
        }
        else modelName = model.get().getName();
        return Component.translatable(this.getDescriptionId(pStack), modelName);
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
        DataModelRegistry.INSTANCE.getValues().stream().sorted(Comparator.comparing(DataModel::getId)).forEach(model -> {
            ItemStack s = new ItemStack(this);
            DataModelItem.setStoredModel(s, model);
            output.accept(s);
        });
    }

}
