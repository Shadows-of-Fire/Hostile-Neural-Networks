package dev.shadowsoffire.hostilenetworks.jei;

import java.text.DecimalFormat;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.util.Color;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class SimChamberCategory implements IRecipeCategory<TickingDataModelWrapper> {

    public static final RecipeType<TickingDataModelWrapper> TYPE = RecipeType.create(HostileNetworks.MODID, "sim_chamber", TickingDataModelWrapper.class);
    public static final ResourceLocation TEXTURES = HostileNetworks.loc("textures/jei/sim_chamber.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component name;

    private int ticks = 0;
    private long lastTickTime = 0;
    static List<TickingDataModelWrapper> recipes;
    private ModelTier currentTier = ModelTier.BASIC;

    public SimChamberCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURES, 0, 0, 116, 43);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Hostile.Items.SIM_CHAMBER));
        this.name = Component.translatable(Hostile.Blocks.SIM_CHAMBER.value().getDescriptionId());
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
    public RecipeType<TickingDataModelWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TickingDataModelWrapper recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).addIngredient(VanillaTypes.ITEM_STACK, recipe.model);
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 4).addIngredients(recipe.input);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 4).addIngredient(VanillaTypes.ITEM_STACK, recipe.baseDrop);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 26).addIngredient(VanillaTypes.ITEM_STACK, recipe.prediction);
    }

    @Override
    public void draw(TickingDataModelWrapper recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long time = mc.level.getGameTime();

        int width = Mth.ceil(35F * (this.ticks % 40 + mc.getTimer().getGameTimeDeltaPartialTick(true)) / 40);

        gfx.blit(TEXTURES, 52, 9, 0, 43, width, 6, 256, 256);

        if (time != this.lastTickTime) {
            if (++this.ticks % 30 == 0) {
                ModelTier next = this.currentTier.next();
                if (next == this.currentTier) next = ModelTier.BASIC;
                for (TickingDataModelWrapper t : recipes)
                    t.setTier(next);
                this.currentTier = next;
            }
            this.lastTickTime = time;
        }
        Component comp = recipe.currentTier.getComponent();
        width = font.width(comp);
        gfx.drawString(font, recipe.currentTier.getComponent(), 33 - width / 2, 30, recipe.currentTier.color());
        DecimalFormat fmt = new DecimalFormat("##.##%");
        String msg = fmt.format(recipe.currentTier.accuracy());
        width = font.width(msg);
        gfx.drawString(font, msg, 114 - width, 30, Color.WHITE, true);
    }

}
