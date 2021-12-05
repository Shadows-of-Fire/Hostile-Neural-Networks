package shadows.hostilenetworks.jei;

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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileNetworks;

public class LootFabCategory implements IRecipeCategory<LootFabRecipe> {

	public static final ResourceLocation UID = new ResourceLocation(HostileNetworks.MODID, "loot_fabricator");

	private final IDrawable background;
	private final IDrawable icon;
	private final String localizedName;

	private int ticks = 0;
	private long lastTickTime = 0;

	public LootFabCategory(IGuiHelper guiHelper) {
		ResourceLocation location = new ResourceLocation(HostileNetworks.MODID, "textures/jei/loot_fabricator.png");
		this.background = guiHelper.createDrawable(location, 0, 0, 103, 30);
		this.icon = guiHelper.createDrawableIngredient(new ItemStack(Hostile.Blocks.LOOT_FABRICATOR));
		this.localizedName = Translator.translateToLocal(Hostile.Blocks.LOOT_FABRICATOR.getDescriptionId());
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
	public Class<LootFabRecipe> getRecipeClass() {
		return LootFabRecipe.class;
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
	public void setIngredients(LootFabRecipe recipe, IIngredients ing) {
		ing.setInput(VanillaTypes.ITEM, recipe.input);
		ing.setOutput(VanillaTypes.ITEM, recipe.output);
	}

	@Override
	public void setRecipe(IRecipeLayout layout, LootFabRecipe recipe, IIngredients ing) {
		IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 8, 6);
		stacks.init(1, false, 78, 6);
		stacks.set(ing);
	}

	@Override
	public void draw(LootFabRecipe recipe, MatrixStack matrixStack, double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		long time = mc.level.getGameTime();
		int width = MathHelper.ceil(36F * (this.ticks % 40 + mc.getDeltaFrameTime()) / 40);
		AbstractGui.blit(matrixStack, 34, 12, 0, 30, width, 6, 256, 256);
		if (time != this.lastTickTime) {
			++this.ticks;
			this.lastTickTime = time;
		}
	}

}