package dev.shadowsoffire.hostilenetworks.data;

import java.util.Arrays;

import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ModelTier {
    FAULTY(0, 1, "faulty", ChatFormatting.DARK_GRAY, 0.01F),
    BASIC(6, 4, "basic", ChatFormatting.GREEN, 0.05F),
    ADVANCED(6 + 48, 10, "advanced", ChatFormatting.BLUE, 0.22F),
    SUPERIOR(6 + 48 + 300, 18, "superior", ChatFormatting.LIGHT_PURPLE, 0.65F),
    SELF_AWARE(6 + 48 + 300 + 900, 0, "self_aware", ChatFormatting.GOLD, 1.0F);

    private static final ModelTier[] VALUES = ModelTier.values();

    private final int data, dataPerKill;
    public final String name;
    public final ChatFormatting color;
    public final float accuracy;

    private ModelTier(int data, int dataPerKill, String name, ChatFormatting color, float accuracy) {
        this.data = data;
        this.dataPerKill = dataPerKill;
        this.name = name;
        this.color = color;
        this.accuracy = accuracy;
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
        return Component.translatable("hostilenetworks.tier." + this.name).withStyle(this.color);
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
        return Arrays.stream(VALUES).mapToInt(t -> t.data).toArray();
    }

    public static int[] defaultDataPerKill() {
        return Arrays.stream(VALUES).mapToInt(t -> t.dataPerKill).toArray();
    }

}
