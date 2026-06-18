package dev.shadowsoffire.hostilenetworks.client;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.multiblock.DataCenterShell;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.Color;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class DataCenterRenderer implements BlockEntityRenderer<DataCenterTileEntity> {

    private static final float OUTLINE_R = ((Color.AQUA >> 16) & 0xFF) / 255f;
    private static final float OUTLINE_G = ((Color.AQUA >> 8) & 0xFF) / 255f;
    private static final float OUTLINE_B = (Color.AQUA & 0xFF) / 255f;
    private static final float OUTLINE_A = 1.0f;

    private static final float WORLD_SCALE_FACTOR = 1.5f;
    private static final float DISPLAY_SCALE_FACTOR = 0.55f;
    private static final float CYCLE_TICKS = 100f;
    private static final float RAMP_FRACTION = 0.15f;
    private static final float HOLD_END_FRACTION = 0.45f;
    private static final float ALIVE_END_FRACTION = 0.60f;

    private static final long SEED_MIX_SLOT = 0x9E3779B97F4A7C15L;
    private static final long SEED_MIX_CYCLE = 0xC6BC279692B5C323L;

    private static final MultiBufferSource.BufferSource GHOST_BUFFER = MultiBufferSource.immediate(new ByteBufferBuilder(256));

    public DataCenterRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(DataCenterTileEntity tile) {
        DataCenterShell.Layout layout = tile.getCachedLayout();
        if (layout == null) return new AABB(tile.getBlockPos());
        BlockPos min = layout.shellMin();
        BlockPos max = layout.shellMax();
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    @Override
    public void render(DataCenterTileEntity tile, float partialTick, PoseStack pose, MultiBufferSource bufs, int light, int overlay) {
        if (!tile.isShellValid()) return;
        DataCenterShell.Layout layout = tile.getCachedLayout();
        if (layout == null) return;

        drawShellOutline(tile, layout, pose, bufs);
        drawDisplaySlots(tile, layout, partialTick, pose);
    }

    private void drawShellOutline(DataCenterTileEntity tile, DataCenterShell.Layout layout, PoseStack pose, MultiBufferSource bufs) {
        BlockPos here = tile.getBlockPos();
        double minX = layout.shellMin().getX() - here.getX();
        double minY = layout.shellMin().getY() - here.getY();
        double minZ = layout.shellMin().getZ() - here.getZ();
        double maxX = layout.shellMax().getX() - here.getX() + 1;
        double maxY = layout.shellMax().getY() - here.getY() + 1;
        double maxZ = layout.shellMax().getZ() - here.getZ() + 1;

        VertexConsumer lines = bufs.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, minX, minY, minZ, maxX, maxY, maxZ, OUTLINE_R, OUTLINE_G, OUTLINE_B, OUTLINE_A);
    }

    @SuppressWarnings("deprecation")
    private void drawDisplaySlots(DataCenterTileEntity tile, DataCenterShell.Layout layout, float partialTick, PoseStack pose) {
        if (tile.displaySlotPositions == null) generateSlotLayout(tile);

        int activeMask = tile.getActiveSlotsMask();
        if (activeMask == 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockPos here = tile.getBlockPos();
        BlockPos center = layout.centerPos();
        float cx = (center.getX() - here.getX()) + 0.5f;
        float cy = (center.getY() - here.getY()) + 0.5f;
        float cz = (center.getZ() - here.getZ()) + 0.5f;

        float baseSpin = ((mc.player != null ? mc.player.tickCount : 0) + partialTick) * 2f;
        float now = mc.level.getGameTime() + partialTick;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        WeirdRenderThings.translucent = true;
        try {
            final float pt = partialTick;
            final int finalActiveMask = activeMask;
            RenderSystem.runAsFancy(() -> {
                for (int i = 0; i < DataCenterTileEntity.DISPLAY_SLOT_COUNT; i++) {
                    drawOneDisplaySlot(tile, i, finalActiveMask, now, baseSpin, cx, cy, cz, pt, pose, dispatcher, mc);
                }
            });
            GHOST_BUFFER.endBatch();
        }
        finally {
            WeirdRenderThings.translucent = false;
            dispatcher.setRenderShadow(true);
        }
    }

    private void drawOneDisplaySlot(DataCenterTileEntity tile, int slotIdx, int activeMask, float now, float baseSpin,
        float cx, float cy, float cz, float partialTick, PoseStack pose, EntityRenderDispatcher dispatcher, Minecraft mc) {

        float localTime = now - tile.displaySlotPhaseOffsets[slotIdx];
        int cycleIdx = Mth.floor(localTime / CYCLE_TICKS);
        float t = (localTime - cycleIdx * CYCLE_TICKS) / CYCLE_TICKS;
        float envelope = envelopeAt(t);
        if (envelope <= 0.001f) return;

        // Picked entity index is locked for the full cycle; if the slot empties mid-cycle we just stop rendering it.
        if (tile.displaySlotCycleIdx[slotIdx] != cycleIdx) {
            tile.displaySlotCycleIdx[slotIdx] = cycleIdx;
            tile.displaySlotEntityIdx[slotIdx] = pickEntityForCycle(activeMask, tile.getBlockPos(), slotIdx, cycleIdx);
        }
        int entityIdx = tile.displaySlotEntityIdx[slotIdx];
        if (entityIdx < 0 || entityIdx >= DataCenterTileEntity.MODEL_SLOTS) return;

        ItemStack stack = tile.getInventory().getStackInSlot(entityIdx);
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(stack);
        if (!model.isBound()) return;
        DisplayEntity display = model.get().displayEntity(mc.level);
        Entity ent = ClientEntityCache.computeIfAbsent(display, mc.level);
        if (ent == null) return;

        if (mc.player != null) ent.tickCount = mc.player.tickCount;

        ent.setYRot(0);
        if (ent instanceof LivingEntity living) {
            living.yBodyRot = 0;
            living.yBodyRotO = 0;
            living.yHeadRot = 0;
            living.yHeadRotO = 0;
        }

        float[] off = tile.displaySlotPositions[slotIdx];
        float scale = WORLD_SCALE_FACTOR * DISPLAY_SCALE_FACTOR * display.scale() * envelope;
        float spin = baseSpin + (slotIdx * 360f / DataCenterTileEntity.DISPLAY_SLOT_COUNT);

        pose.pushPose();
        pose.translate(cx + off[0], cy + off[1], cz + off[2]);
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        dispatcher.render(ent, display.xOffset(), display.yOffset(), display.zOffset(),
            0f, partialTick, pose, new WrappedRTBuffer(GHOST_BUFFER), 0xF000F0);
        pose.popPose();
    }

    private static float envelopeAt(float t) {
        if (t < RAMP_FRACTION) return t / RAMP_FRACTION;
        if (t < HOLD_END_FRACTION) return 1f;
        if (t < ALIVE_END_FRACTION) return (ALIVE_END_FRACTION - t) / RAMP_FRACTION;
        return 0f;
    }

    private static int pickEntityForCycle(int activeMask, BlockPos pos, int slotIdx, int cycleIdx) {
        if (activeMask == 0) return -1;
        int count = Integer.bitCount(activeMask);
        long seed = pos.asLong() ^ (slotIdx * SEED_MIX_SLOT) ^ (cycleIdx * SEED_MIX_CYCLE);
        int pick = new Random(seed).nextInt(count);
        int seen = 0;
        for (int i = 0; i < DataCenterTileEntity.MODEL_SLOTS; i++) {
            if ((activeMask & (1 << i)) == 0) continue;
            if (seen == pick) return i;
            seen++;
        }
        return -1;
    }

    private static void generateSlotLayout(DataCenterTileEntity tile) {
        BlockPos pos = tile.getBlockPos();
        Random rng = new Random(pos.asLong());
        float[][] positions = new float[DataCenterTileEntity.DISPLAY_SLOT_COUNT][3];
        float[] phases = new float[DataCenterTileEntity.DISPLAY_SLOT_COUNT];
        int idx = 0;
        for (int xi = 0; xi < 3; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    float cellCx = -1.33f + xi * 1.33f;
                    float cellCy = -1f + yi * 2f;
                    float cellCz = -1f + zi * 2f;
                    positions[idx][0] = cellCx + (rng.nextFloat() - 0.5f) * 0.8f;
                    positions[idx][1] = cellCy + (rng.nextFloat() - 0.5f) * 1.4f;
                    positions[idx][2] = cellCz + (rng.nextFloat() - 0.5f) * 1.4f;
                    phases[idx] = rng.nextFloat() * CYCLE_TICKS;
                    idx++;
                }
            }
        }
        tile.displaySlotPositions = positions;
        tile.displaySlotPhaseOffsets = phases;
    }
}
