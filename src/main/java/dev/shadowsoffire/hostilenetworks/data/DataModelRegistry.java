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

    private Multimap<EntityType<?>, EntityDataModel> modelsByType = HashMultimap.create();

    public DataModelRegistry() {
        super(HostileNetworks.LOGGER, "data_models", true, true);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(HostileNetworks.loc("entity_data_model"), EntityDataModel.CODEC);
        this.registerCodec(HostileNetworks.loc("block_data_model"), BlockDataModel.CODEC);
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
        if (model instanceof EntityDataModel entityModel) { // TODO: validate() method on DataModel
            entityModel.entityAndVariants().forEach(type -> {
                Collection<EntityDataModel> existingModels = this.modelsByType.get(type);
                if (existingModels.isEmpty()) {
                    // If there are no models, we just take the new one.
                    this.modelsByType.put(type, entityModel);
                }
                else if (existingModels.size() == 1) {
                    // If there's only one model, it might not have an attunement, so validate that they both do.
                    EntityDataModel existing = existingModels.iterator().next();
                    if (!existing.hasAttunement() || !entityModel.hasAttunement()) {
                        throwAttunementError(key, type, existingModels);
                    }
                    this.modelsByType.put(type, entityModel);
                }
                else {
                    // If there's more than one model, we know all the existing ones do, so we only need to check the new one.
                    if (!entityModel.hasAttunement()) {
                        throwAttunementError(key, type, existingModels);
                    }
                    this.modelsByType.put(type, entityModel);
                }
            });
        }
    }

    @Nullable
    public Collection<EntityDataModel> getForEntity(EntityType<?> type) {
        return this.modelsByType.get(type);
    }

    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return super.prepare(pResourceManager, pProfiler);
    }

    private void throwAttunementError(ResourceLocation key, EntityType<?> type, Collection<EntityDataModel> existingModels) {
        String msg = "Attempted to register multiple models for Entity Type %s without specifying an attunement. When registering multiple models, ALL models must specify an attunement!";
        msg += " Existing models: " + existingModels.stream().map(this::getKey).toList();
        msg += " New model: " + key;
        throw new UnsupportedOperationException(String.format(msg, EntityType.getKey(type)));
    }
}
