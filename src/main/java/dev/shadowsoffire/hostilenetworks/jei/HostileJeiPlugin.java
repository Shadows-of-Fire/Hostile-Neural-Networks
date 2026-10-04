package dev.shadowsoffire.hostilenetworks.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class HostileJeiPlugin implements IModPlugin {

    public static final Identifier UID = HostileNetworks.loc("plugin");

    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        reg.registerSubtypeInterpreter(Hostile.Items.DATA_MODEL.value(), new ModelSubtypes());
        reg.registerSubtypeInterpreter(Hostile.Items.PREDICTION.value(), new ModelSubtypes());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new SimChamberCategory(reg.getJeiHelpers().getGuiHelper()));
        reg.addRecipeCategories(new LootFabCategory(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        SimChamberCategory.recipes = DataModelRegistry.INSTANCE.getValues().stream().map(TickingDataModelWrapper::new).collect(Collectors.toList());
        reg.addRecipes(SimChamberCategory.TYPE, SimChamberCategory.recipes);
        List<LootFabRecipe> fabRecipes = new ArrayList<>();
        for (DataModel dm : DataModelRegistry.INSTANCE.getValues()) {
            for (int i = 0; i < dm.fabDrops().size(); i++) {
                fabRecipes.add(new LootFabRecipe(dm, i));
            }
        }
        reg.addRecipes(LootFabCategory.TYPE, fabRecipes);

        // Each call to addItemStackInfo creates a new page.
        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER.value()),
            Component.translatable("hostilenetworks.jei.data_center.intro"));

        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER.value()),
            Component.translatable("hostilenetworks.jei.data_center.shell"));

        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER.value()),
            Component.translatable("hostilenetworks.jei.data_center.operation"));

        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER.value()),
            Component.translatable("hostilenetworks.jei.data_center.io_port"));

        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER_IO_PORT.value()),
            Component.translatable("hostilenetworks.jei.io_port.intro"));

        reg.addItemStackInfo(new ItemStack(Hostile.Blocks.DATA_CENTER_IO_PORT.value()),
            Component.translatable("hostilenetworks.jei.io_port.modes"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addCraftingStation(SimChamberCategory.TYPE, new ItemStack(Hostile.Blocks.SIM_CHAMBER.value()));
        reg.addCraftingStation(LootFabCategory.TYPE, new ItemStack(Hostile.Blocks.LOOT_FABRICATOR.value()));
    }

    private static class ModelSubtypes implements ISubtypeInterpreter<ItemStack> {

        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            DynamicHolder<DataModel> dm = DataModelItem.getStoredModel(stack);
            if (!dm.isBound()) return "NULL";
            return dm.getId();
        }

    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

}
