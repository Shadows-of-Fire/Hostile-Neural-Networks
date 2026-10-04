package dev.shadowsoffire.hostilenetworks.jei;

import java.text.DecimalFormat;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class SimChamberCategory implements IRecipeCategory<TickingDataModelWrapper> {

    public static final IRecipeType<TickingDataModelWrapper> TYPE = IRecipeType.create(HostileNetworks.MODID, "sim_chamber", TickingDataModelWrapper.class);
    public static final Identifier TEXTURES = HostileNetworks.loc("textures/jei/sim_chamber.png");
    private static final int WIDTH = 116;
    private static final int HEIGHT = 43;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component name;

    private int ticks = -1;
    private long lastTickTime = -1;
    static List<TickingDataModelWrapper> recipes;
    private DynamicHolder<ModelTier> currentTier = ModelTierRegistry.INSTANCE.emptyHolder();

    public SimChamberCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURES, 0, 0, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Hostile.Items.SIM_CHAMBER));
        this.name = Component.translatable(Hostile.Blocks.SIM_CHAMBER.value().getDescriptionId());
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
    public IRecipeType<TickingDataModelWrapper> getRecipeType() {
        return TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TickingDataModelWrapper recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).add(recipe.model);
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 4).add(recipe.input);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 96, 4).add(recipe.baseDrop);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 66, 26).add(recipe.prediction);
    }

    @Override
    public void draw(TickingDataModelWrapper recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor gfx, double mouseX, double mouseY) {
        this.background.draw(gfx);

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long time = mc.level.getGameTime();

        int width = Mth.ceil(35F * (this.ticks % 40 + mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 40);

        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURES, 52, 9, 0F, 43F, width, 6, 256, 256);

        if (!this.currentTier.isBound()) {
            this.currentTier = ModelTierRegistry.INSTANCE.holder(ModelTierRegistry.getMinTier());
        }

        if (time != this.lastTickTime) {
            if (++this.ticks % 50 == 0) {
                ModelTier tier = this.currentTier.get();
                ModelTier next;

                if (tier.isMax()) {
                    next = ModelTierRegistry.getMinTier();
                }
                else {
                    next = ModelTierRegistry.next(tier);
                }

                while (!next.canSim()) {
                    next = ModelTierRegistry.next(next);
                }

                for (TickingDataModelWrapper t : recipes) {
                    t.setTier(next);
                }

                this.currentTier = ModelTierRegistry.INSTANCE.holder(next);
            }
            this.lastTickTime = time;
        }

        Component comp = recipe.currentTier.getComponent();
        width = font.width(comp);
        gfx.text(font, recipe.currentTier.getComponent(), 33 - width / 2, 30, (0xFF000000 | recipe.currentTier.colorValue()));
        DecimalFormat fmt = new DecimalFormat("##.##%");
        String msg = fmt.format(recipe.currentTier.accuracy());
        width = font.width(msg);
        gfx.text(font, msg, 114 - width, 30, (0xFF000000 | Color.WHITE), true);
    }

}
