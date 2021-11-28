package shadows.hostilenetworks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;

@Mixin(RenderComponentsUtil.class)
public class MixinRenderComponentsUtil {

	//TODO: Remove
	@Redirect(method = "wrapComponents", at = @At(value = "FIELD", target = "net/minecraft/util/text/Style.EMPTY:Lnet/minecraft/util/text/Style;", ordinal = 1), require = 1)
	private static Style getTrueStyle(ITextProperties text, int maxWidth, FontRenderer font) {
		return text instanceof IFormattableTextComponent ? ((IFormattableTextComponent) text).getStyle() : Style.EMPTY;
	}

}
