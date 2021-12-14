package shadows.hostilenetworks.net;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.placebo.network.MessageHelper;
import shadows.placebo.network.MessageProvider;

public class DataModelResetMessage implements MessageProvider<DataModelResetMessage> {

	@Override
	public void write(DataModelResetMessage msg, FriendlyByteBuf buf) {

	}

	@Override
	public DataModelResetMessage read(FriendlyByteBuf buf) {
		return new DataModelResetMessage();
	}

	@Override
	public void handle(DataModelResetMessage msg, Supplier<Context> ctx) {
		MessageHelper.handlePacket(() -> () -> {
			DataModelManager.INSTANCE.clear();
		}, ctx);
	}

}
