package dev.shadowsoffire.hostilenetworks.datagen;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class BlockTagProvider extends BlockTagsProvider {

    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, HostileNetworks.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(Hostile.Tags.DATA_CENTER_FLOOR).addTag(Tags.Blocks.OBSIDIANS);
        this.tag(Hostile.Tags.DATA_CENTER_WALL)
            .add(Blocks.BLACK_STAINED_GLASS)
            .add(Hostile.Blocks.DATA_CENTER_IO_PORT.value())
            .addOptionalTag(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("chipped", "black_stained_glass")));
        // Optional entries from other mods are added by id; the intrinsic appender only accepts resolved blocks.
        TagBuilder wall = this.getOrCreateRawBuilder(Hostile.Tags.DATA_CENTER_WALL);
        wall.addOptionalElement(Identifier.fromNamespaceAndPath("connectedglass", "borderless_glass_black"));
        wall.addOptionalElement(Identifier.fromNamespaceAndPath("connectedglass", "scratched_glass_black"));
        wall.addOptionalElement(Identifier.fromNamespaceAndPath("connectedglass", "clear_glass_black"));
        wall.addOptionalElement(Identifier.fromNamespaceAndPath("connectedglass", "tinted_borderless_glass_black"));
        wall.addOptionalElement(Identifier.fromNamespaceAndPath("securitycraft", "reinforced_black_stained_glass"));
    }
}
