package dev.shadowsoffire.hostilenetworks;

import java.util.Optional;
import java.util.function.Supplier;

import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.network.MessageProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

public class HostileConfig {

    public static int simPowerCap;
    public static int fabPowerCap;
    public static int fabPowerCost;

    public static boolean rightClickToAttune;
    public static int simModelUpgrade;
    public static boolean killModelUpgrade;
    public static boolean continuousAccuracy;

    public static void load() {
        Configuration cfg = new Configuration(HostileNetworks.MODID);
        cfg.setTitle("Hostile Networks Config");
        cfg.setComment("All entries in this config file are synced from server to client.");
        simPowerCap = cfg.getInt("Sim Chamber Power Cap", "power", 2000000, 1, Integer.MAX_VALUE, "The maximum FE stored in the Simulation Chamber.");
        fabPowerCap = cfg.getInt("Loot Fab Power Cap", "power", 1000000, 1, Integer.MAX_VALUE, "The maximum FE stored in the Loot Fabricator.");
        fabPowerCost = cfg.getInt("Loot Fab Power Cost", "power", 256, 0, Integer.MAX_VALUE, "The FE/t cost of the Loot Fabricator.");

        rightClickToAttune = cfg.getBoolean("Right Click To Attune", "models", true,
            "If true, right clicking a blank data model on a mob will attune it to that mob. If disabled, you will need to provide players with a way to get attuned models!");
        simModelUpgrade = cfg.getInt("Sim Chamber Upgrades Model", "models", 1, 0, 2, "Whether the Simulation Chamber will upgrade the data on a model. (0 = No, 1 = Yes, 2 = Only up to tier boundaries)");
        killModelUpgrade = cfg.getBoolean("Killing Upgrades Model", "models", true,
            "Whether killing mobs will upgrade the data on a model. Note: If you disable this, be sure to add a way for players to get non-Faulty models!");
        continuousAccuracy = cfg.getBoolean("Continuous Accuracy", "models", true,
            "If true, the accuracy of the model increases as it gains progress towards the next tier. If false, always uses the base accuracy of the current tier.");
        if (cfg.hasChanged()) cfg.save();
    }

    static record ConfigMessage(int simPowerCap, int fabPowerCap, int fabPowerCost, boolean rightClickAttune, int simModelUpgrade, boolean killModelUpgrade, boolean continuousAccuracy) {

        public ConfigMessage() {
            this(HostileConfig.simPowerCap, HostileConfig.fabPowerCap, HostileConfig.fabPowerCost, HostileConfig.rightClickToAttune, HostileConfig.simModelUpgrade, HostileConfig.killModelUpgrade, HostileConfig.continuousAccuracy);
        }

        public static class Provider implements MessageProvider<ConfigMessage> {

            @Override
            public Class<?> getMsgClass() {
                return ConfigMessage.class;
            }

            @Override
            public void write(ConfigMessage msg, FriendlyByteBuf buf) {
                buf.writeInt(msg.simPowerCap);
                buf.writeInt(msg.fabPowerCap);
                buf.writeInt(msg.fabPowerCost);
                buf.writeBoolean(msg.rightClickAttune);
                buf.writeInt(msg.simModelUpgrade);
                buf.writeBoolean(msg.killModelUpgrade);
                buf.writeBoolean(msg.continuousAccuracy);
            }

            @Override
            public ConfigMessage read(FriendlyByteBuf buf) {
                return new ConfigMessage(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readBoolean());
            }

            @Override
            public void handle(ConfigMessage msg, Supplier<Context> ctx) {
                MessageHelper.handlePacket(() -> {
                    HostileConfig.simPowerCap = msg.simPowerCap;
                    HostileConfig.fabPowerCap = msg.fabPowerCap;
                    HostileConfig.fabPowerCost = msg.fabPowerCost;
                    HostileConfig.rightClickToAttune = msg.rightClickAttune;
                    HostileConfig.simModelUpgrade = msg.simModelUpgrade;
                    HostileConfig.killModelUpgrade = msg.killModelUpgrade;
                    HostileConfig.continuousAccuracy = msg.continuousAccuracy;
                }, ctx);
            }

            @Override
            public Optional<NetworkDirection> getNetworkDirection() {
                return Optional.of(NetworkDirection.PLAY_TO_CLIENT);
            }

        }

    }

}
