package dev.shadowsoffire.hostilenetworks.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.shadowsoffire.hostilenetworks.Hostile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless geometry + validation for the Data Center's 7×7×7 hollow shell: obsidian floor, glass walls + ceiling, with
 * the controller substituting one bottom-wall position. {@link Layout#wallFace} is the inward direction (controller's
 * {@code FACING.getOpposite()}); the chamber centre is two blocks above the controller. Detection iterates the 7
 * orthogonal candidates along the wall fixed by FACING and validates each.
 */
public final class DataCenterShell {

    public static final int RADIUS = 3;
    public static final int SHELL_SIZE = 2 * RADIUS + 1;
    public static final int CONTROLLER_Y_OFFSET = 1;

    private DataCenterShell() {}

    public static Optional<Layout> findFor(BlockPos controllerPos, BlockState controllerState, LevelReader level) {
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) return Optional.empty();
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        Direction wallFace = facing.getOpposite();

        int floorY = controllerPos.getY() - CONTROLLER_Y_OFFSET;
        int wallX = controllerPos.getX();
        int wallZ = controllerPos.getZ();

        if (wallFace.getAxis() == Direction.Axis.Z) {
            int sz = wallFace == Direction.SOUTH ? wallZ : wallZ - (SHELL_SIZE - 1);
            for (int off = 0; off < SHELL_SIZE; off++) {
                int sx = wallX - off;
                Layout candidate = buildLayout(controllerPos, wallFace, sx, floorY, sz);
                if (validate(candidate, level)) return Optional.of(candidate);
            }
        }
        else {
            int sx = wallFace == Direction.EAST ? wallX : wallX - (SHELL_SIZE - 1);
            for (int off = 0; off < SHELL_SIZE; off++) {
                int sz = wallZ - off;
                Layout candidate = buildLayout(controllerPos, wallFace, sx, floorY, sz);
                if (validate(candidate, level)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static Layout buildLayout(BlockPos controllerPos, Direction wallFace, int sx, int sy, int sz) {
        BlockPos shellMin = new BlockPos(sx, sy, sz);
        BlockPos shellMax = new BlockPos(sx + SHELL_SIZE - 1, sy + SHELL_SIZE - 1, sz + SHELL_SIZE - 1);
        BlockPos centerPos = new BlockPos(sx + RADIUS, sy + RADIUS, sz + RADIUS);
        return new Layout(controllerPos, wallFace, shellMin, shellMax, centerPos);
    }

    /** Short-circuits on the first violation. Use {@link #findInvalidPositions} for a full list. */
    public static boolean validate(Layout layout, LevelReader level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = layout.shellMin.getX(), minY = layout.shellMin.getY(), minZ = layout.shellMin.getZ();
        int maxX = layout.shellMax.getX(), maxY = layout.shellMax.getY(), maxZ = layout.shellMax.getZ();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    boolean isFloor = y == minY;
                    boolean isCeiling = y == maxY;
                    boolean onPerimeter = x == minX || x == maxX || z == minZ || z == maxZ;

                    if (isFloor) {
                        if (!state.is(Hostile.Tags.DATA_CENTER_FLOOR)) return false;
                    }
                    else if (isCeiling) {
                        if (!state.is(Hostile.Tags.DATA_CENTER_WALL)) return false;
                    }
                    else if (onPerimeter) {
                        if (cursor.equals(layout.controllerPos)) continue;
                        if (!state.is(Hostile.Tags.DATA_CENTER_WALL)) return false;
                    }
                    else {
                        // Strict isAir() so replaceable plants/snow don't clip the central mob.
                        if (!state.isAir()) return false;
                    }
                }
            }
        }
        return true;
    }

    public record Layout(BlockPos controllerPos, Direction wallFace, BlockPos shellMin, BlockPos shellMax, BlockPos centerPos) {

        public static void writeLayout(CompoundTag tag, DataCenterShell.Layout layout) {
            tag.putInt("wallFaceOrd", layout.wallFace().get3DDataValue());
            tag.putInt("shellMinX", layout.shellMin().getX());
            tag.putInt("shellMinY", layout.shellMin().getY());
            tag.putInt("shellMinZ", layout.shellMin().getZ());
        }

        public static DataCenterShell.Layout readLayout(CompoundTag tag, BlockPos controllerPos) {
            if (!tag.contains("wallFaceOrd") || !tag.contains("shellMinX")) return null;
            Direction wallFace = Direction.from3DDataValue(tag.getInt("wallFaceOrd"));
            BlockPos shellMin = new BlockPos(tag.getInt("shellMinX"), tag.getInt("shellMinY"), tag.getInt("shellMinZ"));
            BlockPos shellMax = shellMin.offset(DataCenterShell.SHELL_SIZE - 1, DataCenterShell.SHELL_SIZE - 1, DataCenterShell.SHELL_SIZE - 1);
            BlockPos centerPos = shellMin.offset(DataCenterShell.RADIUS, DataCenterShell.RADIUS, DataCenterShell.RADIUS);
            return new DataCenterShell.Layout(controllerPos, wallFace, shellMin, shellMax, centerPos);
        }

    }

    /** Capped per-shell sample size for {@link #findInvalidPositions}. */
    public static final int MAX_REPORTED_INVALID = 50;

    public enum InvalidType {
        OBSIDIAN_MISSING("obsidian_missing"),
        GLASS_MISSING("glass_missing"),
        NOT_AIR("not_air");

        private final String name;

        InvalidType(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.gui.data_center.shell_invalid." + this.name;
        }
    }

    public record InvalidEntry(BlockPos pos, InvalidType type) {}

    /** Returns up to {@link #MAX_REPORTED_INVALID} failing positions. Empty when the shell is valid. */
    public static List<InvalidEntry> findInvalidPositions(Layout layout, LevelReader level) {
        List<InvalidEntry> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = layout.shellMin.getX(), minY = layout.shellMin.getY(), minZ = layout.shellMin.getZ();
        int maxX = layout.shellMax.getX(), maxY = layout.shellMax.getY(), maxZ = layout.shellMax.getZ();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    boolean isFloor = y == minY;
                    boolean isCeiling = y == maxY;
                    boolean onPerimeter = x == minX || x == maxX || z == minZ || z == maxZ;

                    InvalidType problem = null;
                    if (isFloor) {
                        if (!state.is(Hostile.Tags.DATA_CENTER_FLOOR)) problem = InvalidType.OBSIDIAN_MISSING;
                    }
                    else if (isCeiling) {
                        if (!state.is(Hostile.Tags.DATA_CENTER_WALL)) problem = InvalidType.GLASS_MISSING;
                    }
                    else if (onPerimeter) {
                        if (cursor.equals(layout.controllerPos)) continue;
                        if (!state.is(Hostile.Tags.DATA_CENTER_WALL)) problem = InvalidType.GLASS_MISSING;
                    }
                    else {
                        if (!state.isAir()) problem = InvalidType.NOT_AIR;
                    }

                    if (problem != null) {
                        out.add(new InvalidEntry(cursor.immutable(), problem));
                        if (out.size() >= MAX_REPORTED_INVALID) return out;
                    }
                }
            }
        }
        return out;
    }

    /** Returns the candidate with the fewest failures, so the screen can still describe issues when no candidate fully validates. */
    public static Optional<Layout> bestGuessLayout(BlockPos controllerPos, BlockState controllerState, LevelReader level) {
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) return Optional.empty();
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        Direction wallFace = facing.getOpposite();

        int floorY = controllerPos.getY() - CONTROLLER_Y_OFFSET;
        int wallX = controllerPos.getX();
        int wallZ = controllerPos.getZ();

        Layout best = null;
        int bestInvalid = Integer.MAX_VALUE;

        if (wallFace.getAxis() == Direction.Axis.Z) {
            int sz = wallFace == Direction.SOUTH ? wallZ : wallZ - (SHELL_SIZE - 1);
            for (int off = 0; off < SHELL_SIZE; off++) {
                int sx = wallX - off;
                Layout candidate = buildLayout(controllerPos, wallFace, sx, floorY, sz);
                int invalidCount = findInvalidPositions(candidate, level).size();
                if (invalidCount < bestInvalid) {
                    bestInvalid = invalidCount;
                    best = candidate;
                    if (invalidCount == 0) return Optional.of(best);
                }
            }
        }
        else {
            int sx = wallFace == Direction.EAST ? wallX : wallX - (SHELL_SIZE - 1);
            for (int off = 0; off < SHELL_SIZE; off++) {
                int sz = wallZ - off;
                Layout candidate = buildLayout(controllerPos, wallFace, sx, floorY, sz);
                int invalidCount = findInvalidPositions(candidate, level).size();
                if (invalidCount < bestInvalid) {
                    bestInvalid = invalidCount;
                    best = candidate;
                    if (invalidCount == 0) return Optional.of(best);
                }
            }
        }
        return Optional.ofNullable(best);
    }

}
