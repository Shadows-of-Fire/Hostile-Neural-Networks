package dev.shadowsoffire.hostilenetworks.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A snapshot of a Loot Fabricator's configuration - per-model {@link FabSelection}s plus the machine's
 * {@link RedstoneState}, used as the data component copied and pasted by the Fabrication Directive.
 */
public class SavedSelections {

    /**
     * Per-value codec accepting either the current {@link FabSelection} form or a legacy bare int index (which is
     * migrated to a {@link FabSelection#fixed Fixed selection}). This keeps Directives saved before production modes valid.
     */
    private static final Codec<FabSelection> VALUE_CODEC = Codec.either(FabSelection.CODEC, Codec.intRange(0, 65536))
        .xmap(e -> e.map(s -> s, FabSelection::fixed), Either::left);

    private static final Codec<Map<DynamicHolder<DataModel>, FabSelection>> MAP_CODEC = Codec.unboundedMap(DataModelRegistry.INSTANCE.holderCodec(), VALUE_CODEC);

    /** Current shape: {@code {selections: { ... }, redstone_state: "..."}}. */
    private static final Codec<SavedSelections> NEW_CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            MAP_CODEC.optionalFieldOf("selections", Map.<DynamicHolder<DataModel>, FabSelection>of()).forGetter(s -> s.selections),
            RedstoneState.CODEC.optionalFieldOf("redstone_state", RedstoneState.IGNORED).forGetter(s -> s.redstoneState))
        .apply(inst, SavedSelections::new));

    /** Legacy shape: the bare per-model selection map, pre-dating redstone control. Defaults the new redstone field. */
    private static final Codec<SavedSelections> LEGACY_CODEC = MAP_CODEC.xmap(
        m -> new SavedSelections(m, RedstoneState.IGNORED),
        s -> s.selections);

    public static final Codec<SavedSelections> CODEC = Codec.either(NEW_CODEC, LEGACY_CODEC).xmap(Either::unwrap, Either::left);

    public static final StreamCodec<RegistryFriendlyByteBuf, SavedSelections> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, DataModelRegistry.INSTANCE.holderStreamCodec(), FabSelection.STREAM_CODEC),
        s -> s.selections,
        RedstoneState.STREAM_CODEC,
        s -> s.redstoneState,
        SavedSelections::new);

    public static final SavedSelections EMPTY = new SavedSelections(new HashMap<>(), RedstoneState.IGNORED);

    private final Map<DynamicHolder<DataModel>, FabSelection> selections = new HashMap<>();
    private final RedstoneState redstoneState;

    public SavedSelections(Map<DynamicHolder<DataModel>, FabSelection> selections, RedstoneState redstoneState) {
        this.selections.putAll(selections);
        this.redstoneState = redstoneState;
    }

    public Map<DynamicHolder<DataModel>, FabSelection> getSelections() {
        return Collections.unmodifiableMap(this.selections);
    }

    public RedstoneState getRedstoneState() {
        return this.redstoneState;
    }

    /** True only when the directive carries no meaningful state - no selections and the default redstone setting. */
    public boolean isEmpty() {
        return this.selections.isEmpty() && this.redstoneState == RedstoneState.IGNORED;
    }

    public int size() {
        return this.selections.size();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SavedSelections ss && this.selections.equals(ss.selections) && this.redstoneState == ss.redstoneState;
    }

    @Override
    public int hashCode() {
        return 31 * this.selections.hashCode() + this.redstoneState.hashCode();
    }

}
