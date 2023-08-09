package dev.shadowsoffire.hostilenetworks.data;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.world.entity.EntityType;

public class DataModelRegistry extends DynamicRegistry<DataModel> {

    public static final DataModelRegistry INSTANCE = new DataModelRegistry();

    private Map<EntityType<?>, DataModel> modelsByType = new HashMap<>();

    public DataModelRegistry() {
        super(HostileNetworks.LOGGER, "data_models", true, false);
    }

    @Override
    protected void registerBuiltinSerializers() {
        this.registerSerializer(DEFAULT, DataModel.SERIALIZER);
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
    protected void validateItem(DataModel model) {
        model.validate();
        if (this.modelsByType.containsKey(model.type)) {
            String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
            throw new UnsupportedOperationException(String.format(msg, model.getId(), this.modelsByType.get(model.type).getId(), EntityType.getKey(model.type)));
        }
        this.modelsByType.put(model.type, model);
    }

    @Nullable
    public DataModel getForEntity(EntityType<?> type) {
        return this.modelsByType.get(type);
    }

}
