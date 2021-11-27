package shadows.hostilenetworks.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;

public class TickableText {

	protected int ticks = 0;
	protected final String message;
	protected final int color;

	public TickableText(String message, int color) {
		this.message = message;
		this.color = color;
	}

	public void tick() {
		this.ticks++;
	}

	public void render(FontRenderer font, MatrixStack stack, int x, int y) {
		font.draw(stack, message.substring(0, Math.min(ticks, message.length())), x, y, color);
	}

	public void reset() {
		this.ticks = 0;
	}

	public boolean isDone() {
		return ticks >= message.length();
	}
}
