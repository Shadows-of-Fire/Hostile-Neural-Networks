package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;
import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.client.DataModelItemStackRenderer;
import dev.shadowsoffire.hostilenetworks.data.CachedModel;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class DataModelItem extends Item implements ITabFiller {

    public static final String DATA_MODEL = "data_model";
    public static final String ID = "id";
    public static final String DATA = "data";
    public static final String ITERATIONS = "iterations";

    public DataModelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> list, TooltipFlag pFlag) {
        if (Screen.hasShiftDown()) {
            CachedModel cModel = new CachedModel(pStack, 0);
            if (!cModel.isValid()) {
                list.add(Component.translatable("Error: %s", Component.literal("Broke_AF").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.GRAY)));
                return;
            }
            int data = getData(pStack);
            ModelTier tier = ModelTier.getByData(cModel.getModel(), data);
            list.add(Component.translatable("hostilenetworks.info.tier", tier.getComponent()));
            int dProg = data - cModel.getTierData();
            int dMax = cModel.getNextTierData() - cModel.getTierData();
            if (tier != ModelTier.SELF_AWARE) {
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
            List<EntityType<? extends LivingEntity>> subtypes = cModel.getModel().subtypes();
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
    public void fillItemCategory(CreativeModeTab tab, CreativeModeTab.Output output) {
        DataModelRegistry.INSTANCE.getKeys().stream().sorted().forEach(key -> {
            ItemStack s = new ItemStack(this);
            setStoredModel(s, key);
            output.accept(s);
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
     * @return A holder pointing to the nbt-encoded data model.
     */
    public static DynamicHolder<DataModel> getStoredModel(ItemStack stack) {
        if (!stack.hasTag()) return DataModelRegistry.INSTANCE.holder(new ResourceLocation("empty", "empty"));
        String dmKey = stack.getOrCreateTagElement(DATA_MODEL).getString(ID);
        return DataModelRegistry.INSTANCE.holder(new ResourceLocation(dmKey));
    }

    public static void setStoredModel(ItemStack stack, DataModel model) {
        setStoredModel(stack, DataModelRegistry.INSTANCE.getKey(model));
    }

    public static void setStoredModel(ItemStack stack, ResourceLocation model) {
        stack.removeTagKey(DATA_MODEL);
        stack.getOrCreateTagElement(DATA_MODEL).putString(ID, model.toString());
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
        DynamicHolder<DataModel> dModel = getStoredModel(model);
        if (!dModel.isBound()) return false;
        ItemStack input = dModel.get().input();
        boolean item = input.getItem() == stack.getItem();
        if (input.hasTag()) {
            if (stack.hasTag()) {
                CompoundTag t1 = input.getTag();
                CompoundTag t2 = stack.getTag();
                for (String s : t1.getAllKeys()) {
                    if (!t1.get(s).equals(t2.get(s))) return false;
                }
                return true;
            }
            else return false;
        }
        else return item;
    }

}
