package dev.shadowsoffire.hostilenetworks.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.shadowsoffire.hostilenetworks.client.WeirdRenderThings;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.class)
public abstract class MixinRenderType {

    @Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V", shift = Shift.AFTER), require = 1)
    public void hnn_setupNoLighting(BufferBuilder pBuffer, VertexSorting quadSorting, CallbackInfo ci) {
        WeirdRenderThings.setup();
    }

    @Inject(method = "end", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V", shift = Shift.BEFORE), require = 1)
    public void hnn_cleanNoLighting(BufferBuilder pBuffer, VertexSorting quadSorting, CallbackInfo ci) {
        WeirdRenderThings.cleanup();
    }

}
