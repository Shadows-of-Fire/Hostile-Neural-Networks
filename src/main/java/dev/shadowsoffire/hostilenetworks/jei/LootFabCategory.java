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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class LootFabCategory implements IRecipeCategory<LootFabRecipe> {

    public static final IRecipeType<LootFabRecipe> TYPE = IRecipeType.create(HostileNetworks.MODID, "loot_fabricator", LootFabRecipe.class);
    public static final Identifier TEXTURES = HostileNetworks.loc("textures/jei/loot_fabricator.png");
    private static final int WIDTH = 103;
    private static final int HEIGHT = 30;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component name;

    private int ticks = 0;
    private long lastTickTime = 0;

    public LootFabCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURES, 0, 0, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Hostile.Items.LOOT_FABRICATOR));
        this.name = Component.translatable(Hostile.Blocks.LOOT_FABRICATOR.value().getDescriptionId());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
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
    public IRecipeType<LootFabRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LootFabRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 9, 7).add(recipe.input);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 7).add(recipe.output);
    }

    @Override
    public void draw(LootFabRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor gfx, double mouseX, double mouseY) {
        this.background.draw(gfx);
        Minecraft mc = Minecraft.getInstance();
        long time = mc.level.getGameTime();
        int width = Mth.ceil(36F * (this.ticks % 40 + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 40);
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURES, 34, 12, 0F, 30F, width, 6, 256, 256);
        if (time != this.lastTickTime) {
            ++this.ticks;
            this.lastTickTime = time;
        }
    }

}
