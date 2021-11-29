package shadows.hostilenetworks.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.RenderState;

public class WeirdRenderThings {

	public static boolean fullbright = false;
	public static boolean translucent = false;

	public static final RenderState.DiffuseLightingState DIFFUSE = new RenderState.DiffuseLightingState(true);

	public static final RenderState.TransparencyState TRANSLUCENT_TRANSPARENCY = new RenderState.TransparencyState("translucent_transparency", () -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}, () -> {
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	});

	public static void setup() {
		if (fullbright) {
			DIFFUSE.clearRenderState();
		}
		if (translucent) {
			TRANSLUCENT_TRANSPARENCY.setupRenderState();
		}
	}

	public static void cleanup() {
		if (translucent) {
			TRANSLUCENT_TRANSPARENCY.clearRenderState();
		}
	}
}
