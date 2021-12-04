package shadows.hostilenetworks.net;

import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.NetworkUtils.MessageProvider;

public class DataModelMessage extends MessageProvider<DataModelMessage> {

	protected DataModel dm;

	public DataModelMessage() {

	}

	public DataModelMessage(DataModel dm) {
		this.dm = dm;
	}

	@Override
	public void write(DataModelMessage msg, PacketBuffer buf) {
		msg.dm.write(buf);
	}

	@Override
	public DataModelMessage read(PacketBuffer buf) {
		return new DataModelMessage(DataModel.read(buf));
	}

	@Override
	public void handle(DataModelMessage msg, Supplier<Context> ctx) {
		NetworkUtils.handlePacket(() -> () -> {
			DataModelManager.INSTANCE.register(msg.dm);
		}, ctx.get());
	}

}
