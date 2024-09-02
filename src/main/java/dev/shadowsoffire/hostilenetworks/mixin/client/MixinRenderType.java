package dev.shadowsoffire.hostilenetworks.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.MeshData;

import dev.shadowsoffire.hostilenetworks.client.WeirdRenderThings;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.class)
public abstract class MixinRenderType {

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V", shift = Shift.AFTER), require = 1)
    public void hnn_setupNoLighting(MeshData meshData, CallbackInfo ci) {
        WeirdRenderThings.setup();
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V", shift = Shift.BEFORE), require = 1)
    public void hnn_cleanNoLighting(MeshData meshData, CallbackInfo ci) {
        WeirdRenderThings.cleanup();
    }

}
