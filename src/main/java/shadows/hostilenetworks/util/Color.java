package shadows.hostilenetworks.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class Color {
	public static final int AQUA = 0x62D8FF;
	public static final int WHITE = 0xFFFFFF;
	public static final int LIME = 0x00FFC0;
	public static final int BRIGHT_LIME = 0x33EFDC;
	public static final int BRIGHT_PURPLE = 0xC768DB;

	public static Component withColor(String key, int color) {
		Style style = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(color));
		return Component.translatable(key).withStyle(style);
	}
}