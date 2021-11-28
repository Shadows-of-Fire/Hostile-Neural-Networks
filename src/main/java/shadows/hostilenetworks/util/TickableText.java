package shadows.hostilenetworks.util;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;

public class TickableText {

	protected int ticks = 0;
	protected final String message;
	protected final int color;
	protected final boolean newline;
	protected final int tickRate;

	public TickableText(String message, int color, boolean newline, int tickRate) {
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

	public void render(FontRenderer font, MatrixStack stack, int x, int y) {
		font.draw(stack, message.substring(0, Math.min(ticks * tickRate, message.length())), x, y, color);
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
}
