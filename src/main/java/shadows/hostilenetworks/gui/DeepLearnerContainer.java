package shadows.hostilenetworks.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.Hand;
import shadows.hostilenetworks.Hostile;

public class DeepLearnerContainer extends Container {

	protected final Hand hand;

	public DeepLearnerContainer(int pContainerId, PlayerInventory pPlayerInventory, Hand hand) {
		super(Hostile.Containers.DEEP_LEARNER, pContainerId);
		this.hand = hand;
	}

	@Override
	public boolean stillValid(PlayerEntity pPlayer) {
		return false;
	}

}
