package shadows.hostilenetworks.jei;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.util.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.util.Color;

public class SimChamberCategory implements IRecipeCategory<TickingDataModelWrapper> {

	public static final ResourceLocation UID = new ResourceLocation(HostileNetworks.MODID, "sim_chamber");

	private final IDrawable background;
	private final IDrawable icon;
	private final String localizedName;

	private int ticks = 0;
	private long lastTickTime = 0;
	static List<TickingDataModelWrapper> recipes;
	private ModelTier currentTier = ModelTier.BASIC;

	public SimChamberCategory(IGuiHelper guiHelper) {
		ResourceLocation location = new ResourceLocation(HostileNetworks.MODID, "textures/jei/sim_chamber.png");
		this.background = guiHelper.createDrawable(location, 0, 0, 116, 43);
		this.icon = guiHelper.createDrawableIngredient(new ItemStack(Hostile.Blocks.SIM_CHAMBER));
		this.localizedName = Translator.translateToLocal(Hostile.Blocks.SIM_CHAMBER.getDescriptionId());
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
	public Class<TickingDataModelWrapper> getRecipeClass() {
		return TickingDataModelWrapper.class;
	}

	@Override
	public String getTitle() {
		return this.localizedName;
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public void setIngredients(TickingDataModelWrapper recipe, IIngredients ing) {
		ing.setInputs(VanillaTypes.ITEM, Arrays.asList(recipe.model, recipe.input));
		ing.setOutputs(VanillaTypes.ITEM, Arrays.asList(recipe.baseDrop, recipe.prediction));
	}

	@Override
	public void setRecipe(IRecipeLayout layout, TickingDataModelWrapper recipe, IIngredients ing) {
		IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 3, 3);
		stacks.init(1, true, 27, 3);
		stacks.init(2, false, 95, 3);
		stacks.init(3, false, 65, 25);
		stacks.set(ing);
	}

	@Override
	public void draw(TickingDataModelWrapper recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		FontRenderer font = mc.font;
		long time = mc.level.getGameTime();

		int width = MathHelper.ceil(35F * (this.ticks % 40 + mc.getDeltaFrameTime()) / 40);

		AbstractGui.blit(matrixStack, 52, 9, 0, 43, width, 6, 256, 256);

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
		ITextComponent comp = recipe.currentTier.getComponent();
		width = font.width(comp);
		font.draw(matrixStack, recipe.currentTier.getComponent(), 33 - width / 2, 30, recipe.currentTier.color.getColor());
		DecimalFormat fmt = new DecimalFormat("##.##%");
		String msg = fmt.format(recipe.currentTier.accuracy);
		width = font.width(msg);
		font.drawShadow(matrixStack, msg, 114 - width, 30, Color.WHITE);
	}

}