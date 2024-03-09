package dev.shadowsoffire.hostilenetworks.data;

import com.google.common.base.Preconditions;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.ModelTier.TierData;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;

public class ModelTierRegistry extends DynamicRegistry<TierData> {

    public static final ModelTierRegistry INSTANCE = new ModelTierRegistry();

    public ModelTierRegistry() {
        super(HostileNetworks.LOGGER, "model_tiers", true, false);

        for (ModelTier tier : ModelTier.values()) {
            holder(HostileNetworks.loc(tier.name));
        }
    }

    @Override
    protected void onReload() {
        super.onReload();
        // check that the builtin tiers and only the builtin tiers exist
        for (ModelTier tier : ModelTier.values()) {
            String name = tier.name;
            Preconditions.checkNotNull(getValue(HostileNetworks.loc(name)), "Missing builtin model tier: " + name);
        }

        Preconditions.checkArgument(this.getValues().size() == ModelTier.values().length, "Registration of additional model tiers is currently not supported!");

        // update the builtin tiers (separate step so that we don't have a half-updated state if validation fails)
        for (ModelTier tier : ModelTier.values()) {
            tier.updateData(this.getValue(HostileNetworks.loc(tier.name)));
        }
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(HostileNetworks.loc("tier_data"), TierData.CODEC);
    }

    @Override
    protected void validateItem(ResourceLocation key, TierData value) {
        super.validateItem(key, value);
        if (isTier(key, ModelTier.FAULTY)) {
            Preconditions.checkArgument(value.requiredData() == 0, "Faulty tier cannot require any data!");
        } else if (isTier(key, ModelTier.SELF_AWARE)) {
            Preconditions.checkArgument(value.dataPerKill() == 0, "Self-aware tier cannot be upgraded further!");
        }
    }

    private boolean isTier(ResourceLocation key, ModelTier tier) {
        return key.getPath().equals(tier.name);
    }
}
