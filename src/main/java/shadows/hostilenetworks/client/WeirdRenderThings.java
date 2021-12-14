package shadows.hostilenetworks.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.RenderStateShard;

public class WeirdRenderThings {

	public static boolean fullbright_gui = false;
	public static boolean fullbright_tesr = false;
	public static boolean translucent = false;

	private static final Vector3f ITEM_V1 = new Vector3f(-1.0F, 0.0F, 1.0F);
	private static final Vector3f ITEM_V2 = new Vector3f(1F, 1F, 0.0F);
	private static final Vector3f GUI_V1 = new Vector3f(0.2F, -1.0F, -1.0F);
	private static final Vector3f GUI_V2 = new Vector3f(-0.2F, -1.0F, 0.0F);
	static {
		GUI_V1.normalize();
		GUI_V2.normalize();
	}

	public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}, () -> {
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	});

	public static void setup() {
		if (fullbright_gui) {
			RenderSystem.setShaderLights(GUI_V1, GUI_V2);
		} else if (fullbright_tesr) {
			RenderSystem.setShaderLights(ITEM_V1, ITEM_V2);
		}
		if (translucent) {
			TRANSLUCENT_TRANSPARENCY.setupRenderState();
		}
	}

	public static void cleanup() {
		if (translucent) {
			TRANSLUCENT_TRANSPARENCY.clearRenderState();
		}
		if (fullbright_gui || fullbright_tesr) {
			Lighting.setupFor3DItems();
		}
	}
}
