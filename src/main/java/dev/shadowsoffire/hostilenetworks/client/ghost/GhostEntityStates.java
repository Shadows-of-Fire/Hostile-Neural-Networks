package dev.shadowsoffire.hostilenetworks.client.ghost;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Extracts {@link EntityRenderState}s for ghost rendering of cached display entities.
 * <p>
 * The extracted state is normalized so that the ghost renders fullbright, without shadow, name tag, fire, or leash, with
 * zeroed rotations (spin is applied via the pose stack by the caller), and without the bob / spin of item entities.
 */
public final class GhostEntityStates {

    public static final int FULL_BRIGHT = 0xF000F0;

    private static final Set<EntityType<?>> FAILED = Collections.newSetFromMap(new IdentityHashMap<>());

    private GhostEntityStates() {}

    /**
     * Extracts a normalized render state for the given entity.
     *
     * @param entity      A cached client-side display entity (see {@code ClientEntityCache}).
     * @param partialTick The current partial tick.
     * @return The render state, or null if the entity has no renderer or extraction failed.
     */
    @Nullable
    public static EntityRenderState extract(Entity entity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        if (dispatcher.getRenderer(entity) == null) return null;

        // Keep animations (idle flaps, swimming, etc.) advancing at the world's rate.
        if (mc.player != null) entity.tickCount = mc.player.tickCount;

        EntityRenderState state;
        try {
            state = dispatcher.extractEntity(entity, partialTick);
        }
        catch (RuntimeException ex) {
            // A broken third-party renderer must not take every inventory screen down with it.
            if (FAILED.add(entity.getType())) {
                HostileNetworks.LOGGER.error("Failed to extract the render state of ghost entity {}", EntityType.getKey(entity.getType()), ex);
            }
            return null;
        }

        state.lightCoords = FULL_BRIGHT;
        state.nameTag = null;
        state.scoreText = null;
        state.nameTagAttachment = null;
        state.displayFireAnimation = false;
        state.leashStates = null;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 0;
            living.yRot = 0;
            living.xRot = 0;
            living.isInvisibleToPlayer = false;
        }

        if (state instanceof ItemEntityRenderState item) {
            // ItemEntityRenderer derives both the spin and the bob from these, so zeroing them yields a static item.
            item.ageInTicks = 0;
            item.bobOffset = 0;
            item.shouldBob = false;
        }

        return state;
    }

}
