package shadows.hostilenetworks.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.placebo.network.MessageHelper;
import shadows.placebo.network.MessageProvider;

public class DataModelMessage implements MessageProvider<DataModelMessage> {

	protected DataModel dm;

	public DataModelMessage() {

	}

	public DataModelMessage(DataModel dm) {
		this.dm = dm;
	}

	@Override
	public void write(DataModelMessage msg, FriendlyByteBuf buf) {
		msg.dm.write(buf);
	}

	@Override
	public DataModelMessage read(FriendlyByteBuf buf) {
		return new DataModelMessage(DataModel.read(buf));
	}

	@Override
	public void handle(DataModelMessage msg, Supplier<Context> ctx) {
		MessageHelper.handlePacket(() -> () -> {
			DataModelManager.INSTANCE.register(msg.dm);
		}, ctx);
	}

}
