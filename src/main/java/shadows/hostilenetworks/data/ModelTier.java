package shadows.hostilenetworks.data;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public enum ModelTier {
	FAULTY(0, 1, "faulty", TextFormatting.DARK_GRAY, 0.01F),
	BASIC(6, 4, "basic", TextFormatting.GREEN, 0.05F),
	ADVANCED(6 + 48, 10, "advanced", TextFormatting.BLUE, 0.22F),
	SUPERIOR(6 + 48 + 300, 18, "superior", TextFormatting.LIGHT_PURPLE, 0.65F),
	SELF_AWARE(6 + 48 + 300 + 900, 0, "self_aware", TextFormatting.GOLD, 0.995F);

	private static final ModelTier[] VALUES = ModelTier.values();

	public final int data, dataPerKill;
	public final String name;
	public final TextFormatting color;
	public final float accuracy;

	private ModelTier(int data, int dataPerKill, String name, TextFormatting color, float accuracy) {
		this.data = data;
		this.dataPerKill = dataPerKill;
		this.name = name;
		this.color = color;
		this.accuracy = accuracy;
	}

	public ModelTier previous() {
		if (this == FAULTY) return this;
		return VALUES[this.ordinal() - 1];
	}

	public ModelTier next() {
		if (this == SELF_AWARE) return this;
		return VALUES[this.ordinal() + 1];
	}

	public ITextComponent getComponent() {
		return new TranslationTextComponent("hostilenetworks.tier." + this.name).withStyle(this.color);
	}

	public static ModelTier getByData(int data) {
		for (int i = 4; i >= 0; i--) {
			if (data >= VALUES[i].data) return VALUES[i];
		}
		return FAULTY;
	}

}
