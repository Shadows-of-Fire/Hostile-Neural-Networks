package dev.shadowsoffire.hostilenetworks.util;

import net.minecraft.util.StringRepresentable;

public enum IOPortMode implements StringRepresentable {
    ENERGY("energy"),
    MODELS("models"),
    INPUTS("inputs"),
    OUTPUTS("outputs");

    private static final IOPortMode[] VALUES = values();

    private final String name;

    IOPortMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getTranslationKey() {
        return "hostilenetworks.io_port.mode." + this.name;
    }

    public IOPortMode cycle() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }
}
