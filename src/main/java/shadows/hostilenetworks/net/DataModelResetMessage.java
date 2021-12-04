package shadows.hostilenetworks.net;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.NetworkUtils.MessageProvider;

public class DataModelResetMessage extends MessageProvider<DataModelResetMessage> {

	@Override
	public void write(DataModelResetMessage msg, PacketBuffer buf) {

	}

	@Override
	public DataModelResetMessage read(PacketBuffer buf) {
		return new DataModelResetMessage();
	}

	@Override
	public void handle(DataModelResetMessage msg, Supplier<Context> ctx) {
		NetworkUtils.handlePacket(() -> () -> {
			DataModelManager.INSTANCE.clear();
		}, ctx.get());
	}

}
