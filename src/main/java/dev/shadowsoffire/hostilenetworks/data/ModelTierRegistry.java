package dev.shadowsoffire.hostilenetworks.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;

public class ModelTierRegistry extends DynamicRegistry<ModelTier> {

    public static final ModelTierRegistry INSTANCE = new ModelTierRegistry();

    /**
     * Sorted list of model tiers. The first element is the lowest model tier, and the last element is the highest.
     */
    private LinkedList<ModelTier> sorted = new LinkedList<>();

    public ModelTierRegistry() {
        super(HostileNetworks.LOGGER, "model_tiers", true, false);
    }

    public static ModelTier getMaxTier() {
        if (INSTANCE.sorted.isEmpty()) {
            throw new UnsupportedOperationException("Cannot access model tiers before the registry has loaded.");
        }
        return INSTANCE.sorted.peekLast();
    }

    public static ModelTier getMinTier() {
        if (INSTANCE.sorted.isEmpty()) {
            throw new UnsupportedOperationException("Cannot access model tiers before the registry has loaded.");
        }
        return INSTANCE.sorted.peekFirst();
    }

    public static ModelTier getByData(DataModel model, int data) {
        for (ModelTier tier : INSTANCE.sorted.reversed()) {
            if (data >= model.getRequiredData(tier)) {
                return tier;
            }
        }
        return getMinTier();
    }

    public static List<ModelTier> getSortedTiers() {
        return Collections.unmodifiableList(INSTANCE.sorted);
    }

    /**
     * Returns the next tier in sorted order. If the tier is already the max tier, returns itself.
     * <p>
     * If the tier is unknown, returns {@link #getMinTier()}.
     */
    public static ModelTier next(ModelTier tier) {
        if (tier.isMax()) {
            return tier;
        }

        int idx = INSTANCE.sorted.indexOf(tier);
        if (idx == -1) {
            return getMinTier();
        }

        return INSTANCE.sorted.get(idx + 1);
    }

    @Override
    protected void beginReload() {
        super.beginReload();
        this.sorted.clear();
    }

    @Override
    protected void onReload() {
        super.onReload();
        this.registry.values().stream().sorted(Comparator.comparing(ModelTier::requiredData)).forEach(sorted::add);
        ModelTier min = sorted.peekFirst();
        if (min.requiredData() != 0) {
            throw new UnsupportedOperationException("The lowest model tier must have a required data of zero. Currently, the lowest model tier is %s - %s".formatted(this.getKey(min), min));
        }
        int lastData = -1;
        ModelTier last = null;
        for (ModelTier tier : sorted) {
            if (lastData >= tier.requiredData()) {
                throw new UnsupportedOperationException("Invalid model tier ordering detected. "
                    + "The set of tiers must form an ascending list when sorted by required data. "
                    + "Current Error: Model Tier %s has the same required data as tier %s".formatted(this.getKey(last), this.getKey(tier)));
            }
            lastData = tier.requiredData();
        }

        if (this.getValues().stream().noneMatch(ModelTier::canSim)) {
            throw new UnsupportedOperationException("At least one model tier must be valid for use in the simulation chamber.");
        }
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(HostileNetworks.loc("model_tier"), ModelTier.CODEC);
    }

    @Override
    protected void validateItem(ResourceLocation key, ModelTier value) {
        if (!HostileNetworks.MODID.equals(key.getNamespace())) {
            throw new UnsupportedOperationException("Model Tiers must be registered under the `hostilenetworks` namespace.");
        }
    }

}
