package shadows.hostilenetworks.util;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.MathHelper;

public class TickableText {

	protected int ticks = 0;
	protected final String message;
	protected final int color;
	protected final boolean newline;
	protected final float tickRate;

	public TickableText(String message, int color, boolean newline, float tickRate) {
		this.message = message;
		this.color = color;
		this.newline = newline;
		this.tickRate = tickRate;
	}

	public TickableText(String message, int color) {
		this(message, color, true, 1);
	}

	public void tick() {
		this.ticks++;
	}

	public TickableText setTicks(int ticks) {
		this.ticks = ticks;
		return this;
	}

	public void render(FontRenderer font, MatrixStack stack, int x, int y) {
		font.draw(stack, message.substring(0, MathHelper.ceil(Math.min(ticks * tickRate, message.length()))), x, y, color);
	}

	public int getMaxUsefulTicks() {
		return MathHelper.floor(message.length() / tickRate);
	}

	public void reset() {
		this.ticks = 0;
	}

	public boolean isDone() {
		return ticks * tickRate >= message.length();
	}

	public boolean causesNewLine() {
		return this.newline;
	}

	public int getWidth(FontRenderer font) {
		return font.width(this.message);
	}

	public static void tickList(List<TickableText> texts) {
		for (int i = 0; i < texts.size(); i++) {
			TickableText txt = texts.get(i);
			if (!txt.isDone()) {
				txt.tick();
				break;
			}
		}
	}
}
