package dev.shadowsoffire.hostilenetworks;

import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import dev.shadowsoffire.placebo.util.Offset;
import dev.shadowsoffire.placebo.util.Offset.AnchorPoint;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class HostileConfig {

    public static int simPowerCap;
    public static int fabPowerCap;
    public static int fabPowerCost;
    public static int dataCenterPowerCap;

    public static boolean rightClickToAttune;
    public static int simModelUpgrade;
    public static boolean actionUpgradesModel;
    public static boolean continuousAccuracy;
    public static boolean enableBlockDataModels;
    public static float dataCenterSimCostMultiplier;

    public static Offset deepLearnerOffset = new Offset(AnchorPoint.TOP_LEFT, 0, 0);

    public static Configuration load() {
        Configuration cfg = new Configuration(HostileNetworks.MODID);
        cfg.setTitle("Hostile Networks Config");
        cfg.setComment("All entries in this config file are synced from server to client unless noted otherwise.");
        simPowerCap = cfg.getInt("Sim Chamber Power Cap", "power", 2000000, 1, Integer.MAX_VALUE, "The maximum FE stored in the Simulation Chamber.");
        fabPowerCap = cfg.getInt("Loot Fab Power Cap", "power", 1000000, 1, Integer.MAX_VALUE, "The maximum FE stored in the Loot Fabricator.");
        fabPowerCost = cfg.getInt("Loot Fab Power Cost", "power", 256, 0, Integer.MAX_VALUE, "The FE/t cost of the Loot Fabricator.");
        dataCenterPowerCap = cfg.getInt("Data Center Power Cap", "power", 8000000, 1, Integer.MAX_VALUE, "The maximum FE stored in the Data Center. Sized for 25 concurrent self-aware simulations.");

        rightClickToAttune = cfg.getBoolean("Right Click To Attune", "models", true,
            "If true, right clicking a blank data model on a mob or block will attune it to that target. If disabled, you will need to provide players with a way to get attuned models!");
        simModelUpgrade = cfg.getInt("Sim Chamber Upgrades Model", "models", 1, 0, 2,
            "How the Simulation Chamber's Training Mode upgrades the data on a model. (0 = Disabled - also disables Training Mode entirely, 1 = Yes, 2 = Only up to tier boundaries)");
        // Migrate the legacy "Killing Upgrades Model" key (entity-only) to "Action Upgrades Model" (covers kills and block breaks).
        if (!cfg.hasKey("models", "Action Upgrades Model") && cfg.hasKey("models", "Killing Upgrades Model")) {
            cfg.renameProperty("models", "Killing Upgrades Model", "Action Upgrades Model");
        }
        actionUpgradesModel = cfg.getBoolean("Action Upgrades Model", "models", true,
            "Whether killing mobs and breaking blocks will upgrade the data on a corresponding model. Note: If you disable this, be sure to add a way for players to get non-Faulty models!");
        continuousAccuracy = cfg.getBoolean("Continuous Accuracy", "models", true,
            "If true, the accuracy of the model increases as it gains progress towards the next tier. If false, always uses the base accuracy of the current tier.");
        enableBlockDataModels = cfg.getBoolean("Enable Block Data Models", "models", false,
            "If true, block data models (such as the built-in ore models) are loaded. This is an experimental feature. This value is not synced; it governs which models load during datapack reading.");
        dataCenterSimCostMultiplier = cfg.getFloat("Data Center Sim Cost Multiplier", "models", 1.5f, 0.01f, 100f,
            "Per-tick power cost the Data Center pays per active model, as a multiplier on the model's base simCost. 1.5 = 150% of normal.");

        cfg.setCategoryComment("client", "Client-only options, not synced");
        deepLearnerOffset = Offset.load("Deep Learner HUD", "client", deepLearnerOffset, cfg);
        if (cfg.hasChanged()) cfg.save();
        return cfg;
    }

    static record ConfigPayload(int simPowerCap, int fabPowerCap, int fabPowerCost, int dataCenterPowerCap, boolean rightClickAttune, int simModelUpgrade, boolean actionUpgradesModel, boolean continuousAccuracy,
        float dataCenterSimCostMultiplier) implements CustomPacketPayload {

        public static final Type<ConfigPayload> TYPE = new Type<>(HostileNetworks.loc("config"));

        // Hand-rolled because StreamCodec.composite tops out at 7 fields (NeoForgeStreamCodecs.composite).
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeVarInt(msg.simPowerCap);
                buf.writeVarInt(msg.fabPowerCap);
                buf.writeVarInt(msg.fabPowerCost);
                buf.writeVarInt(msg.dataCenterPowerCap);
                buf.writeBoolean(msg.rightClickAttune);
                buf.writeVarInt(msg.simModelUpgrade);
                buf.writeBoolean(msg.actionUpgradesModel);
                buf.writeBoolean(msg.continuousAccuracy);
                buf.writeFloat(msg.dataCenterSimCostMultiplier);
            },
            buf -> new ConfigPayload(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat()));

        public ConfigPayload() {
            this(HostileConfig.simPowerCap, HostileConfig.fabPowerCap, HostileConfig.fabPowerCost, HostileConfig.dataCenterPowerCap, HostileConfig.rightClickToAttune, HostileConfig.simModelUpgrade, HostileConfig.actionUpgradesModel,
                HostileConfig.continuousAccuracy, HostileConfig.dataCenterSimCostMultiplier);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static class Provider implements PayloadProvider<ConfigPayload> {

            @Override
            public Type<ConfigPayload> getType() {
                return TYPE;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ConfigPayload> getCodec() {
                return CODEC;
            }

            @Override
            public void handle(ConfigPayload msg, IPayloadContext ctx) {
                HostileConfig.simPowerCap = msg.simPowerCap;
                HostileConfig.fabPowerCap = msg.fabPowerCap;
                HostileConfig.fabPowerCost = msg.fabPowerCost;
                HostileConfig.dataCenterPowerCap = msg.dataCenterPowerCap;
                HostileConfig.rightClickToAttune = msg.rightClickAttune;
                HostileConfig.simModelUpgrade = msg.simModelUpgrade;
                HostileConfig.actionUpgradesModel = msg.actionUpgradesModel;
                HostileConfig.continuousAccuracy = msg.continuousAccuracy;
                HostileConfig.dataCenterSimCostMultiplier = msg.dataCenterSimCostMultiplier;
            }

            @Override
            public List<ConnectionProtocol> getSupportedProtocols() {
                return List.of(ConnectionProtocol.PLAY);
            }

            @Override
            public Optional<PacketFlow> getFlow() {
                return Optional.of(PacketFlow.CLIENTBOUND);
            }

            @Override
            public String getVersion() {
                return "3";
            }

        }

    }

}
