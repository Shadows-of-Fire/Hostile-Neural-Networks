package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.placebo.config.Configuration;

public class HostileConfig {

    public static int simPowerCap = 2000000;
    public static int fabPowerCap = 1000000;
    public static int fabPowerCost = 256;
    public static boolean continuousAccuracy = false;
    public static int simModelUpgrade = 0;

    public static void load() {
        Configuration cfg = new Configuration(HostileNetworks.MODID);
        simPowerCap = cfg.getInt("Sim Chamber Power Cap", "power", simPowerCap, 1, Integer.MAX_VALUE, "The maximum FE stored in the Simulation Chamber.");
        fabPowerCap = cfg.getInt("Loot Fab Power Cap", "power", fabPowerCap, 1, Integer.MAX_VALUE, "The maximum FE stored in the Loot Fabricator.");
        fabPowerCost = cfg.getInt("Loot Fab Power Cost", "power", fabPowerCost, 0, Integer.MAX_VALUE, "The FE/t cost of the Loot Fabricator.");
        continuousAccuracy = cfg.getBoolean("Continuous Accuracy", "models", continuousAccuracy, "If true, the accuracy of the model increases as it gains progress towards the next tier. If false, always uses the base accuracy of the current tier.");
        simModelUpgrade = cfg.getInt("Sim Chamber Upgrades Model", "models", simModelUpgrade, 0,2, "Whether the Simulation Chamber will upgrade the data on a model. (0 = No, 1 = Yes, 2 = Only up to tier boundaries)");
        if (cfg.hasChanged()) cfg.save();
    }

}
