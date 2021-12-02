package shadows.hostilenetworks.util;

import net.minecraftforge.energy.EnergyStorage;

public class ModifiableEnergyStorage extends EnergyStorage {

	public ModifiableEnergyStorage(int capacity, int maxReceive) {
		super(capacity, maxReceive, 0, 0);
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

}
