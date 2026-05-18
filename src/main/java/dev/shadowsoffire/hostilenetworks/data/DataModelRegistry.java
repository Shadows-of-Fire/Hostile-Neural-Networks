package dev.shadowsoffire.hostilenetworks.data;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.util.DisplayableBlock;
import dev.shadowsoffire.placebo.reload.DynamicRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public class DataModelRegistry extends DynamicRegistry<DataModel> {

    public static final DataModelRegistry INSTANCE = new DataModelRegistry();

    private Multimap<EntityType<?>, EntityDataModel> modelsByType = HashMultimap.create();
    private Multimap<Block, BlockDataModel> modelsByBlock = HashMultimap.create();

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
        this.modelsByBlock = HashMultimap.create();
    }

    @Override
    protected void onReload(ReloadType type) {
        super.onReload(type);
        this.modelsByType = ImmutableMultimap.copyOf(this.modelsByType);
        this.modelsByBlock = ImmutableMultimap.copyOf(this.modelsByBlock);
    }

    /**
     * Validates that the data model is legal in context of the other registered models.
     * <p>
     * A model is legal if it is the only model for its target (entity type or block), or all models for that target specify an attunement.
     * <p>
     * This method places the model into the appropriate by-target lookup if it is valid.
     */
    @Override
    protected void validateItem(ResourceLocation key, DataModel model) {
        switch (model) {
            case EntityDataModel entityModel -> validateEntity(key, entityModel);
            case BlockDataModel blockModel -> validateBlock(key, blockModel);
            default -> {}
        }
    }

    private void validateEntity(ResourceLocation key, EntityDataModel entityModel) {
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
                    throwAttunementError(key, "Entity Type", EntityType.getKey(type), existingModels);
                }
                this.modelsByType.put(type, entityModel);
            }
            else {
                // If there's more than one model, we know all the existing ones do, so we only need to check the new one.
                if (!entityModel.hasAttunement()) {
                    throwAttunementError(key, "Entity Type", EntityType.getKey(type), existingModels);
                }
                this.modelsByType.put(type, entityModel);
            }
        });
    }

    private void validateBlock(ResourceLocation key, BlockDataModel blockModel) {
        Stream<Block> blocks = Stream.concat(
            Stream.of(blockModel.block().block()),
            blockModel.variants().stream().map(DisplayableBlock::block));
        blocks.forEach(block -> {
            Collection<BlockDataModel> existingModels = this.modelsByBlock.get(block);
            if (existingModels.isEmpty()) {
                this.modelsByBlock.put(block, blockModel);
            }
            else if (existingModels.size() == 1) {
                BlockDataModel existing = existingModels.iterator().next();
                if (!existing.hasAttunement() || !blockModel.hasAttunement()) {
                    throwAttunementError(key, "Block", BuiltInRegistries.BLOCK.getKey(block), existingModels);
                }
                this.modelsByBlock.put(block, blockModel);
            }
            else {
                if (!blockModel.hasAttunement()) {
                    throwAttunementError(key, "Block", BuiltInRegistries.BLOCK.getKey(block), existingModels);
                }
                this.modelsByBlock.put(block, blockModel);
            }
        });
    }

    @Nullable
    public Collection<EntityDataModel> getForEntity(EntityType<?> type) {
        return this.modelsByType.get(type);
    }

    @Nullable
    public Collection<BlockDataModel> getForBlock(Block block) {
        return this.modelsByBlock.get(block);
    }

    @Override
    public Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return super.prepare(pResourceManager, pProfiler);
    }

    private void throwAttunementError(ResourceLocation key, String targetKind, ResourceLocation targetId, Collection<? extends DataModel> existingModels) {
        String msg = "Attempted to register multiple models for %s %s without specifying an attunement. When registering multiple models, ALL models must specify an attunement!";
        msg += " Existing models: " + existingModels.stream().map(this::getKey).toList();
        msg += " New model: " + key;
        throw new UnsupportedOperationException(String.format(msg, targetKind, targetId));
    }
}
