package shadows.hostilenetworks.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

public class Color {
	public static final int AQUA = 0x62D8FF;
	public static final int WHITE = 0xFFFFFF;
	public static final int LIME = 0x00FFC0;
	public static final int BRIGHT_LIME = 0x33EFDC;
	public static final int BRIGHT_PURPLE = 0xC768DB;

	public static ITextComponent withColor(String key, int color) {
		Style style = Style.EMPTY.withColor(net.minecraft.util.text.Color.fromRgb(color));
		return new TranslationTextComponent(key).withStyle(style);
	}
}