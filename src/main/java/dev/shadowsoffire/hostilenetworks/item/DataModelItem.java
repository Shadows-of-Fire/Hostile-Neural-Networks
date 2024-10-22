package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;
import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.client.DataModelItemStackRenderer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class DataModelItem extends Item implements ITabFiller {

    public DataModelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            DataModelInstance cModel = new DataModelInstance(stack, 0);
            if (!cModel.isValid()) {
                list.add(Component.translatable("Error: %s", Component.literal("Broke_AF").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.GRAY)));
                return;
            }
            int data = getData(stack);
            ModelTier tier = ModelTierRegistry.getByData(cModel.getModel(), data);
            list.add(Component.translatable("hostilenetworks.info.tier", tier.getComponent()));
            int dProg = data - cModel.getTierData();
            int dMax = cModel.getNextTierData() - cModel.getTierData();
            if (!tier.isMax()) {
                list.add(Component.translatable("hostilenetworks.info.data", Component.translatable("hostilenetworks.info.dprog", dProg, dMax).withStyle(ChatFormatting.GRAY)));
                int dataPerKill = cModel.getDataPerKill();
                if (dataPerKill == 0) {
                    Component c1 = Component.literal("000 ").withStyle(ChatFormatting.GRAY, ChatFormatting.OBFUSCATED);
                    list.add(Component.translatable("hostilenetworks.info.dpk", c1).append(Component.translatable("hostilenetworks.info.disabled").withStyle(ChatFormatting.RED)));
                }
                else {
                    list.add(Component.translatable("hostilenetworks.info.dpk", Component.literal("" + cModel.getDataPerKill()).withStyle(ChatFormatting.GRAY)));
                }
            }
            list.add(Component.translatable("hostilenetworks.info.sim_cost", Component.translatable("hostilenetworks.info.rft", cModel.getModel().simCost()).withStyle(ChatFormatting.GRAY)));
            List<EntityType<?>> subtypes = cModel.getModel().variants();
            if (!subtypes.isEmpty()) {
                list.add(Component.translatable("hostilenetworks.info.subtypes"));
                for (EntityType<?> t : subtypes) {
                    list.add(Component.translatable("hostilenetworks.info.sub_list", t.getDescription()).withStyle(Style.EMPTY.withColor(Color.LIME)));
                }
            }
        }
        else {
            list.add(Component.translatable("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", ChatFormatting.WHITE.getColor())).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, BuildCreativeModeTabContentsEvent event) {
        DataModelRegistry.INSTANCE.getKeys().stream().sorted().map(DataModelRegistry.INSTANCE::holder).forEach(holder -> {
            ItemStack s = new ItemStack(this);
            setStoredModel(s, holder);
            event.accept(s);
        });
    }

    @Override
    public Component getName(ItemStack pStack) {
        DynamicHolder<DataModel> model = getStoredModel(pStack);
        Component modelName;
        if (!model.isBound()) {
            modelName = Component.literal("BROKEN").withStyle(ChatFormatting.OBFUSCATED);
        }
        else modelName = model.get().name();
        return Component.translatable(this.getDescriptionId(pStack), modelName);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            DataModelItemStackRenderer dmisr = new DataModelItemStackRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.dmisr;
            }
        });
    }

    /**
     * @return A holder pointing to the nbt-encoded data model. May be unbound.
     */
    public static DynamicHolder<DataModel> getStoredModel(ItemStack stack) {
        return stack.getOrDefault(Hostile.Components.DATA_MODEL, DataModelRegistry.INSTANCE.emptyHolder());
    }

    public static void setStoredModel(ItemStack stack, DataModel model) {
        setStoredModel(stack, DataModelRegistry.INSTANCE.holder(model));
    }

    public static void setStoredModel(ItemStack stack, DynamicHolder<DataModel> model) {
        stack.set(Hostile.Components.DATA_MODEL, model);
    }

    public static int getData(ItemStack stack) {
        return stack.getOrDefault(Hostile.Components.DATA, 0);
    }

    public static void setData(ItemStack stack, int data) {
        stack.set(Hostile.Components.DATA, data);
    }

    public static int getIters(ItemStack stack) {
        return stack.getOrDefault(Hostile.Components.ITERATIONS, 0);
    }

    public static void setIters(ItemStack stack, int iterations) {
        stack.set(Hostile.Components.ITERATIONS, iterations);
    }

    /**
     * Returns true if the <code>stack</code> matches the input item specified by the {@link DataModel} stored in <code>model</code>.
     */
    public static boolean matchesModelInput(ItemStack model, ItemStack stack) {
        DynamicHolder<DataModel> dModel = getStoredModel(model);
        if (!dModel.isBound()) return false;
        Ingredient input = dModel.get().input();
        return input.test(stack);
    }

}
