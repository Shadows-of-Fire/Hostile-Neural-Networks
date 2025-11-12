package dev.shadowsoffire.hostilenetworks.util;

import org.spongepowered.include.com.google.common.base.Preconditions;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class DisplayableBlock {

    public static final Codec<DisplayableBlock> FULL_CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(b -> b.block),
            ItemStack.CODEC.fieldOf("display_stack").forGetter(DisplayableBlock::displayStack))
        .apply(inst, DisplayableBlock::new));

    public static final Codec<DisplayableBlock> CODEC = Codec.either(BuiltInRegistries.BLOCK.holderByNameCodec(), FULL_CODEC).xmap(DisplayableBlock::unwrap, DisplayableBlock::wrap);

    private final Holder<Block> block;
    private final ItemStack displayStack;

    public DisplayableBlock(Holder<Block> block, ItemStack displayStack) {
        this.block = block;
        this.displayStack = displayStack.copyWithCount(1);
        Preconditions.checkArgument(!this.displayStack.isEmpty(), "Display stack for block %s cannot be empty", block.getKey().location());
    }

    public DisplayableBlock(Holder<Block> block) {
        this(block, new ItemStack(block.value()));
    }

    public Block block() {
        return block.value();
    }

    public ItemStack displayStack() {
        return displayStack.copy();
    }

    private static DisplayableBlock unwrap(Either<Holder<Block>, DisplayableBlock> either) {
        return either.map(DisplayableBlock::new, db -> db);
    }

    private Either<Holder<Block>, DisplayableBlock> wrap() {
        if (ItemStack.isSameItemSameComponents(displayStack, new ItemStack(this.block.value()))) {
            return Either.left(this.block);
        }
        else {
            return Either.right(this);
        }
    }
}
