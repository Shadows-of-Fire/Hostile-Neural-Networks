package dev.shadowsoffire.hostilenetworks.jei;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class LootFabCategory implements IRecipeCategory<LootFabRecipe> {

    public static final RecipeType<LootFabRecipe> TYPE = RecipeType.create(HostileNetworks.MODID, "loot_fabricator", LootFabRecipe.class);
    public static final ResourceLocation TEXTURES = new ResourceLocation(HostileNetworks.MODID, "textures/jei/loot_fabricator.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component name;

    private int ticks = 0;
    private long lastTickTime = 0;

    public LootFabCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURES, 0, 0, 103, 30);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Hostile.Blocks.LOOT_FABRICATOR.get()));
        this.name = Component.translatable(Hostile.Blocks.LOOT_FABRICATOR.get().getDescriptionId());
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public Component getTitle() {
        return this.name;
    }

    @Override
    public RecipeType<LootFabRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LootFabRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 9, 7).addIngredient(VanillaTypes.ITEM_STACK, recipe.input);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 7).addIngredient(VanillaTypes.ITEM_STACK, recipe.output);
    }

    @Override
    public void draw(LootFabRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        long time = mc.level.getGameTime();
        int width = Mth.ceil(36F * (this.ticks % 40 + mc.getDeltaFrameTime()) / 40);
        gfx.blit(TEXTURES, 34, 12, 0, 30, width, 6, 256, 256);
        if (time != this.lastTickTime) {
            ++this.ticks;
            this.lastTickTime = time;
        }
    }

}
