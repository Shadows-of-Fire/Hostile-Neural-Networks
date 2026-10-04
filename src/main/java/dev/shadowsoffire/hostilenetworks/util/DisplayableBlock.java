package dev.shadowsoffire.hostilenetworks.util;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;

public class DisplayableBlock {

    public static final Codec<DisplayableBlock> FULL_CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(b -> b.block),
            ItemStackTemplate.CODEC.fieldOf("display_stack").forGetter(b -> b.explicitStack))
        .apply(inst, DisplayableBlock::new));

    public static final Codec<DisplayableBlock> CODEC = Codec.either(BuiltInRegistries.BLOCK.holderByNameCodec(), FULL_CODEC).xmap(DisplayableBlock::unwrap, DisplayableBlock::wrap);

    private final Holder<Block> block;

    @Nullable
    private final ItemStackTemplate explicitStack;

    public DisplayableBlock(Holder<Block> block, ItemStackTemplate displayStack) {
        this.block = block;
        this.explicitStack = displayStack.withCount(1);
    }

    public DisplayableBlock(Holder<Block> block) {
        this.block = block;
        this.explicitStack = null;
    }

    public Block block() {
        return block.value();
    }

    public ItemStack displayStack() {
        if (this.explicitStack != null) {
            return this.explicitStack.create();
        }
        return new ItemStack(this.block.value());
    }

    private static DisplayableBlock unwrap(Either<Holder<Block>, DisplayableBlock> either) {
        return either.map(DisplayableBlock::new, db -> db);
    }

    private Either<Holder<Block>, DisplayableBlock> wrap() {
        if (this.explicitStack == null) {
            return Either.left(this.block);
        }
        return Either.right(this);
    }
}
