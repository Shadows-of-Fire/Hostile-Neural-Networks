package dev.shadowsoffire.hostilenetworks.client;

import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.config.Configuration;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;

public class Offset {

    public final AnchorPoint anchor;
    public final int x, y;

    public Offset(AnchorPoint anchor, int x, int y) {
        this.anchor = anchor;
        this.x = x;
        this.y = y;
    }

    public int getX(Box window, Box element) {
        return this.anchor.getX(window.width) - this.anchor.getX(element.width) + this.x;
    }

    public int getY(Box window, Box element) {
        return this.anchor.getY(window.height) - this.anchor.getY(element.height) + this.y;
    }

    public void apply(PoseStack pose, Box window, Box element) {
        pose.translate(this.getX(window, element), this.getY(window, element), 0);
    }

    public static Offset load(String key, String group, AnchorPoint def, Configuration cfg) {
        AnchorPoint anchor = AnchorPoint.parse(cfg.getString(key + " Anchor Point", group, def.toString().toLowerCase(Locale.ROOT), "The anchor point for this element."));
        int x = cfg.getInt(key + " X Offset", group, 0, -1000, 1000, "The X offset for this element.");
        int y = cfg.getInt(key + " Y Offset", group, 0, -1000, 1000, "The Y Offset for this element.");
        return new Offset(anchor, x, y);
    }

    public static record Box(int width, int height) {}

    public static enum AnchorPoint {
        TOP_LEFT(width -> 0, height -> 0),
        TOP_CENTER(width -> width / 2, height -> 0),
        TOP_RIGHT(width -> width, height -> 0),
        MIDDLE_LEFT(width -> 0, height -> height / 2),
        MIDDLE_CENTER(width -> width / 2, height -> height / 2),
        MIDDLE_RIGHT(width -> width, height -> height / 2),
        BOTTOM_LEFT(width -> 0, height -> height),
        BOTTOM_CENTER(width -> width / 2, height -> height),
        BOTTOM_RIGHT(width -> width, height -> height);

        public static final Codec<AnchorPoint> CODEC = PlaceboCodecs.enumCodec(AnchorPoint.class);

        private Int2IntFunction xPos;
        private Int2IntFunction yPos;

        private AnchorPoint(Int2IntFunction xPos, Int2IntFunction yPos) {
            this.xPos = xPos;
            this.yPos = yPos;
        }

        public int getX(int width) {
            return this.xPos.apply(width);
        }

        public int getY(int height) {
            return this.yPos.apply(height);
        }

        public static AnchorPoint parse(String s) {
            try {
                return AnchorPoint.valueOf(s.toUpperCase(Locale.ROOT));
            }
            catch (Exception ex) {
                HostileNetworks.LOGGER.error("Failed to parse invalid Anchor Point {}", s);
                ex.printStackTrace();
                return AnchorPoint.TOP_LEFT;
            }
        }
    }
}
