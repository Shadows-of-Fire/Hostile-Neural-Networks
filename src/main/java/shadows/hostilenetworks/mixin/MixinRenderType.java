package shadows.hostilenetworks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import shadows.hostilenetworks.client.WeirdRenderThings;

@Mixin(RenderType.class)
public abstract class MixinRenderType {

	@Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldVertexBufferUploader;end(Lnet/minecraft/client/renderer/BufferBuilder;)V"), require = 1)
	public void setupNoLighting(BufferBuilder pBuffer, int pCameraX, int pCameraY, int pCameraZ, CallbackInfo ci) {
		WeirdRenderThings.setup();
	}

	@Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldVertexBufferUploader;end(Lnet/minecraft/client/renderer/BufferBuilder;)V", shift = Shift.AFTER), require = 1)
	public void cleanNoLighting(BufferBuilder pBuffer, int pCameraX, int pCameraY, int pCameraZ, CallbackInfo ci) {
		WeirdRenderThings.cleanup();
	}

}
