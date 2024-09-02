package dev.shadowsoffire.hostilenetworks.data;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

public class DataModelRegistry extends DynamicRegistry<DataModel> {

    public static final DataModelRegistry INSTANCE = new DataModelRegistry();

    private Map<EntityType<?>, DataModel> modelsByType = new HashMap<>();

    public DataModelRegistry() {
        super(HostileNetworks.LOGGER, "data_models", true, false);
    }

    @Override
    protected void registerBuiltinCodecs() {
        this.registerDefaultCodec(HostileNetworks.loc("data_model"), DataModel.CODEC);
    }

    @Override
    protected void beginReload() {
        super.beginReload();
        this.modelsByType = new HashMap<>();
    }

    @Override
    protected void onReload() {
        super.onReload();
        this.modelsByType = ImmutableMap.copyOf(this.modelsByType);
    }

    @Override
    protected void validateItem(ResourceLocation key, DataModel model) {
        if (this.modelsByType.containsKey(model.entity())) {
            String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
            throw new UnsupportedOperationException(String.format(msg, key, this.getKey(this.modelsByType.get(model.entity())), EntityType.getKey(model.entity())));
        }
        this.modelsByType.put(model.entity(), model);
    }

    @Nullable
    public DataModel getForEntity(EntityType<?> type) {
        return this.modelsByType.get(type);
    }

    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return super.prepare(pResourceManager, pProfiler);
    }

}
