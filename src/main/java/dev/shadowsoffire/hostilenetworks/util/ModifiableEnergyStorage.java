package dev.shadowsoffire.hostilenetworks.util;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Local replacement for Placebo's deleted {@code dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage}.
 * Extends NeoForge's {@link SimpleEnergyHandler} (the 26.1-era replacement for {@code EnergyStorage}) with
 * back-compat helpers the BE tick code calls — {@code getEnergyStored}, {@code setEnergy}, {@code setMaxExtract}.
 */
public class ModifiableEnergyStorage extends SimpleEnergyHandler {

    public ModifiableEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer, maxTransfer);
    }

    public int getEnergyStored() {
        return this.energy;
    }

    public int getMaxEnergyStored() {
        return this.capacity;
    }

    public void setEnergy(int energy) {
        this.set(Math.max(0, Math.min(energy, this.capacity)));
    }

    public void setMaxReceive(int maxReceive) {
        this.maxInsert = maxReceive;
    }

    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
    }
}
