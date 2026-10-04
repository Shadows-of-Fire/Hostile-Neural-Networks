package dev.shadowsoffire.hostilenetworks.client.item;

import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Item model layer that draws the ghost entity of a Data Model item via {@link DataModelGhostRenderer}.
 * <p>
 * Unlike vanilla's {@code minecraft:special} wrapper this keeps the {@link ItemDisplayContext} 
 * (needed for the per-context transforms of the ghost) and always marks the render state as
 * animated, so the GUI item atlas re-renders the icon every frame instead of caching the first one.
 */
public class DataModelItemModel implements ItemModel {

    /** Generous extents so the GUI treats the icon as oversized and does not clip tall mobs at the slot edge. */
    private static final Vector3fc[] EXTENT_ARRAY = { new Vector3f(0, 0, 0), new Vector3f(1, 1.75F, 1) };
    private static final Supplier<Vector3fc[]> EXTENTS = () -> EXTENT_ARRAY;

    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    public DataModelItemModel(ModelRenderProperties properties, Matrix4fc transformation) {
        this.properties = properties;
        this.transformation = new Matrix4f(transformation);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level,
        @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        this.properties.applyToLayer(layer, displayContext);
        layer.setExtents(EXTENTS);
        layer.setLocalTransform(this.transformation);

        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(item);
        layer.setupSpecialModel(DataModelGhostRenderer.INSTANCE, new Argument(model, displayContext));

        // Identity is keyed by model + context (not by stack) so equal items share atlas / picture-in-picture slots.
        output.appendModelIdentityElement(model.getId());
        output.appendModelIdentityElement(displayContext);
        // The ghost spins and animates, so the cached GUI icon must be refreshed every frame.
        output.setAnimated();
    }

    /**
     * Per-render argument handed to {@link DataModelGhostRenderer}.
     *
     * @param model   The stored data model (possibly unbound).
     * @param context The display context the item is rendered in.
     */
    public record Argument(DynamicHolder<DataModel> model, ItemDisplayContext context) {}

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst
            .group(
                Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base))
            .apply(inst, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel model = baker.getModel(this.base);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, model, model.getTopTextureSlots());
            return new DataModelItemModel(properties, transformation);
        }
    }

}
