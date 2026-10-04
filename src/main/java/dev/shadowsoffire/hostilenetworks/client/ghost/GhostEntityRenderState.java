package dev.shadowsoffire.hostilenetworks.client.ghost;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * Picture-in-picture render state for a ghost entity drawn inside a GUI. Mirrors the vanilla {@code GuiEntityRenderState}.
 *
 * @param renderState         The (normalized) entity render state.
 * @param translation         Translation applied in entity space after scaling, before rotation.
 * @param rotation            Rotation applied to the entity.
 * @param overrideCameraAngle Optional camera orientation override.
 * @param entityOffset        Offset passed to the entity render dispatcher (the data model's display offsets).
 * @param alpha               Ghost alpha (0-255).
 * @param x0                  Left edge of the picture, in GUI coordinates.
 * @param y0                  Top edge of the picture.
 * @param x1                  Right edge of the picture.
 * @param y1                  Bottom edge of the picture.
 * @param scale               Pixels per block.
 * @param scissorArea         Active scissor area, if any.
 * @param bounds              Effective bounds (picture intersected with the scissor area).
 */
public record GhostEntityRenderState(EntityRenderState renderState, Vector3f translation, Quaternionf rotation, @Nullable Quaternionf overrideCameraAngle,
    Vector3f entityOffset, int alpha, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
    implements PictureInPictureRenderState {

    public GhostEntityRenderState(EntityRenderState renderState, Vector3f translation, Quaternionf rotation, @Nullable Quaternionf overrideCameraAngle, Vector3f entityOffset,
        int alpha, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
        this(renderState, translation, rotation, overrideCameraAngle, entityOffset, alpha, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

}
