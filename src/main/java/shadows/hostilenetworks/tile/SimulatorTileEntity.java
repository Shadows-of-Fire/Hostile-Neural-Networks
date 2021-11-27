package shadows.hostilenetworks.tile;

import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.HostileConfig;

public class SimulatorTileEntity extends TileEntity implements ITickableTileEntity {

	protected final ItemStackHandler inventory = new ItemStackHandler(4);
	protected final EnergyStorage energy = new EnergyStorage(HostileConfig.simPowerCap, HostileConfig.simPowerCap, 0, 0);

	public SimulatorTileEntity(TileEntityType<?> type) {
		super(type);
	}

	@Override
	public void tick() {
	}

}
