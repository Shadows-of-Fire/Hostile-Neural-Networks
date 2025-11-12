package dev.shadowsoffire.hostilenetworks.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Variant of {@link EntityAttunement} for blocks. See that class for general details.
 */
public record BlockAttunement(boolean attunable, BlockPredicate predicate) {

    public static Codec<BlockAttunement> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            Codec.BOOL.fieldOf("attunable").forGetter(BlockAttunement::attunable),
            BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockAttunement::predicate))
        .apply(inst, BlockAttunement::new));

    /**
     * Checks if the block state at the given position matches the attunement rules.
     * <p>
     * The block will already have been checked to ensure it is of the correct type.
     * 
     * @param level The level containing the block to check.
     * @param pos   The position of the block to check against the attunement rules.
     */
    public boolean matches(ServerLevel level, BlockPos pos) {
        return this.attunable && predicate.matches(level, pos);
    }
}
