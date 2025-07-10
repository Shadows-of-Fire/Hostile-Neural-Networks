package dev.shadowsoffire.hostilenetworks.data;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

public class DataModelRegistry extends DynamicRegistry<DataModel> {

    public static final DataModelRegistry INSTANCE = new DataModelRegistry();

    private Multimap<EntityType<?>, DataModel> modelsByType = HashMultimap.create();

    public DataModelRegistry() {
        super(HostileNetworks.LOGGER, "data_models", true, false);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(HostileNetworks.loc("data_model"), DataModel.CODEC);
    }

    @Override
    protected void beginReload(ReloadType type) {
        super.beginReload(type);
        this.modelsByType = HashMultimap.create();
    }

    @Override
    protected void onReload(ReloadType type) {
        super.onReload(type);
        this.modelsByType = ImmutableMultimap.copyOf(this.modelsByType);
    }

    /**
     * Validates that the data model is legal in context of the other registered models.
     * <p>
     * A model is legal if it is the only model for an entity type, or all models for that type specify an attunement.
     * <p>
     * This method places the model into the map of models by type if it is valid.
     */
    @Override
    protected void validateItem(ResourceLocation key, DataModel model) {
        model.entityAndVariants().forEach(type -> {
            Collection<DataModel> existingModels = this.modelsByType.get(type);
            if (existingModels.isEmpty()) {
                // If there are no models, we just take the new one.
                this.modelsByType.put(type, model);
            }
            else if (existingModels.size() == 1) {
                // If there's only one model, it might not have an attunement, so validate that they both do.
                DataModel existing = existingModels.iterator().next();
                if (!existing.hasAttunement() || !model.hasAttunement()) {
                    throwAttunementError(key, type, existingModels);
                }
                this.modelsByType.put(type, model);
            }
            else {
                // If there's more than one model, we know all the existing ones do, so we only need to check the new one.
                if (!model.hasAttunement()) {
                    throwAttunementError(key, type, existingModels);
                }
                this.modelsByType.put(type, model);
            }
        });
    }

    @Nullable
    public Collection<DataModel> getForEntity(EntityType<?> type) {
        return this.modelsByType.get(type);
    }

    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return super.prepare(pResourceManager, pProfiler);
    }

    private void throwAttunementError(ResourceLocation key, EntityType<?> type, Collection<DataModel> existingModels) {
        String msg = "Attempted to register multiple models for Entity Type %s without specifying an attunement. When registering multiple models, ALL models must specify an attunement!";
        msg += " Existing models: " + existingModels.stream().map(this::getKey).toList();
        msg += " New model: " + key;
        throw new UnsupportedOperationException(String.format(msg, EntityType.getKey(type)));
    }
}
