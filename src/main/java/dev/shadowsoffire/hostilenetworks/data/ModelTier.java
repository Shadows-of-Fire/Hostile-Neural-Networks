package dev.shadowsoffire.hostilenetworks.data;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringRepresentable;

public enum ModelTier implements StringRepresentable {
    FAULTY("faulty", 0, 1, ChatFormatting.DARK_GRAY, 0.01F),
    BASIC("basic", 6, 4, ChatFormatting.GREEN, 0.05F),
    ADVANCED("advanced", 6 + 48, 10, ChatFormatting.BLUE, 0.22F),
    SUPERIOR("superior", 6 + 48 + 300, 18, ChatFormatting.LIGHT_PURPLE, 0.65F),
    SELF_AWARE("self_aware", 6 + 48 + 300 + 900, 0, ChatFormatting.GOLD, 0.995F);

    private static final ModelTier[] VALUES = ModelTier.values();

    public final String name;

    private TierData tierData;

    public static final Codec<ModelTier> CODEC = StringRepresentable.fromEnum(() -> VALUES);

    ModelTier(String name, int requiredData, int dataPerKill, ChatFormatting color, float accuracy) {
        this(name, requiredData, dataPerKill, TextColor.fromLegacyFormat(color), accuracy);
    }

    ModelTier(String name, int requiredData, int dataPerKill, TextColor color, float accuracy) {
        this.name = name;
        this.tierData = new TierData(requiredData, dataPerKill, color, accuracy);
    }

    public ModelTier previous() {
        if (this == FAULTY) return this;
        return VALUES[this.ordinal() - 1];
    }

    public ModelTier next() {
        if (this == SELF_AWARE) return this;
        return VALUES[this.ordinal() + 1];
    }

    public Component getComponent() {
        return Component.translatable("hostilenetworks.tier." + this.name).withStyle(Style.EMPTY.withColor(data().color));
    }

    public static ModelTier getByData(DynamicHolder<DataModel> model, int data) {
        return !model.isBound() ? FAULTY : getByData(model.get(), data);
    }

    public static ModelTier getByData(DataModel model, int data) {
        for (int i = 4; i >= 0; i--) {
            if (data >= model.getTierData(VALUES[i])) return VALUES[i];
        }
        return FAULTY;
    }

    public static int[] defaultData() {
        return Arrays.stream(VALUES).mapToInt(t -> t.data().requiredData).toArray();
    }

    public static int[] defaultDataPerKill() {
        return Arrays.stream(VALUES).mapToInt(t -> t.data().dataPerKill).toArray();
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public TierData data() {
        return tierData;
    }

    public int color() {
        return data().color.getValue();
    }

    public float accuracy() {
        return data().accuracy;
    }

    void updateData(TierData data) {
        this.tierData = data;
    }

    public record TierData(int requiredData, int dataPerKill, TextColor color, float accuracy) implements CodecProvider<TierData> {

        public static final Codec<TierData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("required_data").forGetter(TierData::requiredData),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("data_per_kill").forGetter(TierData::dataPerKill),
            TextColor.CODEC.fieldOf("color").forGetter(TierData::color),
            Codec.floatRange(0, 1).fieldOf("accuracy").forGetter(TierData::accuracy)).apply(inst, TierData::new));

        @Override
        public Codec<? extends TierData> getCodec() {
            return CODEC;
        }
    }
}
