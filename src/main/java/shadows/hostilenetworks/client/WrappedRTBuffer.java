package shadows.hostilenetworks.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;

public class WrappedRTBuffer implements IRenderTypeBuffer {

	private final IRenderTypeBuffer wrapped;

	public WrappedRTBuffer(IRenderTypeBuffer wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public IVertexBuilder getBuffer(RenderType type) {
		return new GhostVertexBuilder(wrapped.getBuffer(type), 0xBB);
	}

}
