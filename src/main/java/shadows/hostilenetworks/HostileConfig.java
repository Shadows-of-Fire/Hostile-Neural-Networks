package shadows.hostilenetworks;

import shadows.placebo.config.Configuration;

public class HostileConfig {

	public static int simPowerCap = 2000000;
	public static int fabPowerCap = 1000000;
	public static int fabPowerCost = 256;

	public static void load() {
		Configuration cfg = new Configuration(HostileNetworks.MODID);
		simPowerCap = cfg.getInt("Sim Chamber Power Cap", "power", simPowerCap, 1, Integer.MAX_VALUE, "The maximum FE stored in the Simulation Chamber.");
		fabPowerCap = cfg.getInt("Loot Fab Power Cap", "power", fabPowerCap, 1, Integer.MAX_VALUE, "The maximum FE stored in the Loot Fabricator.");
		fabPowerCost = cfg.getInt("Loot Fab Power Cost", "power", fabPowerCost, 0, Integer.MAX_VALUE, "The FE/t cost of the Loot Fabricator.");
		if (cfg.hasChanged()) cfg.save();
	}

}
