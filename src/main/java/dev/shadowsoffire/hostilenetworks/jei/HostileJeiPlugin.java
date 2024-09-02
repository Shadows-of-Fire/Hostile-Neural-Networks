package dev.shadowsoffire.hostilenetworks.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class HostileJeiPlugin implements IModPlugin {

    public static final ResourceLocation UID = HostileNetworks.loc("plugin");

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
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(new ItemStack(Hostile.Blocks.SIM_CHAMBER.value()), SimChamberCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(Hostile.Blocks.LOOT_FABRICATOR.value()), LootFabCategory.TYPE);
    }

    private static class ModelSubtypes implements IIngredientSubtypeInterpreter<ItemStack> {

        @Override
        public String apply(ItemStack stack, UidContext context) {
            DynamicHolder<DataModel> dm = DataModelItem.getStoredModel(stack);
            if (!dm.isBound()) return "NULL";
            return dm.getId().toString();
        }

    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

}
