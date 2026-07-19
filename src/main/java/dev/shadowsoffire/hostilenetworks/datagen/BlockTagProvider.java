package dev.shadowsoffire.hostilenetworks.datagen;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class BlockTagProvider extends BlockTagsProvider {

    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFileHelper) {
        super(output, lookup, HostileNetworks.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(Hostile.Tags.DATA_CENTER_FLOOR).addTag(Tags.Blocks.OBSIDIANS);
        this.tag(Hostile.Tags.DATA_CENTER_WALL)
            .add(Blocks.BLACK_STAINED_GLASS)
            .add(Hostile.Blocks.DATA_CENTER_IO_PORT.value())
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("chipped", "black_stained_glass"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("connectedglass", "borderless_glass_black"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("connectedglass", "scratched_glass_black"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("connectedglass", "clear_glass_black"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("connectedglass", "tinted_borderless_glass_black"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("securitycraft", "reinforced_black_stained_glass"));
    }
}
