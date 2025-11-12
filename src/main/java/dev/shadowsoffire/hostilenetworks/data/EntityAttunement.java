package dev.shadowsoffire.hostilenetworks.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A Model Attunement specifies data-driven rules for how a Model Framework should attune to a specific data model.
 * <p>
 * Most data models will not specify an attunement, and will simply match to the entity type they are designed for.
 * <p>
 * However, to support NBT-based entities, certain models may specify an attunement that checks for specific NBT data on the entity.
 * <p>
 * Models with an attunement are exempted from the one-model-per-entity rule.
 * 
 * @param attunable Whether this model can be attuned at all. If false, the predicate is ignored, and the model will require external creation.
 * @param predicate The entity predicate that defines the attunement rules for this model.
 */
public record EntityAttunement(boolean attunable, EntityPredicate predicate) {

    public static Codec<EntityAttunement> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Codec.BOOL.fieldOf("attunable").forGetter(EntityAttunement::attunable),
            EntityPredicate.CODEC.fieldOf("predicate").forGetter(EntityAttunement::predicate))
        .apply(inst, EntityAttunement::new));

    /**
     * Checks if the given entity matches the attunement rules.
     * <p>
     * The entity will already have been checked to ensure it is of the correct type.
     * 
     * @param attuner The player attuning the model, used for context in the predicate.
     * @param entity  The entity to check against the attunement rules.
     */
    public boolean matches(ServerPlayer attuner, Entity entity) {
        return this.attunable && predicate.matches(attuner, entity);
    }
}
