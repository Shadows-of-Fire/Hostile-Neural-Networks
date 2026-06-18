package dev.shadowsoffire.hostilenetworks.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

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
        return findInvalidPositions(layout, level, 1).isEmpty();
    }

    /** Categorises a position inside the shell. Use {@link Layout#classify} to compute. */
    public enum CellKind {
        FLOOR, CEILING, WALL, CONTROLLER, INTERIOR
    }

    public record Layout(BlockPos controllerPos, Direction wallFace, BlockPos shellMin, BlockPos shellMax, BlockPos centerPos) {

        public CellKind classify(int x, int y, int z) {
            if (y == this.shellMin.getY()) return CellKind.FLOOR;
            if (y == this.shellMax.getY()) return CellKind.CEILING;
            boolean onPerimeter = x == this.shellMin.getX() || x == this.shellMax.getX()
                || z == this.shellMin.getZ() || z == this.shellMax.getZ();
            if (!onPerimeter) return CellKind.INTERIOR;
            if (x == this.controllerPos.getX() && y == this.controllerPos.getY() && z == this.controllerPos.getZ()) return CellKind.CONTROLLER;
            return CellKind.WALL;
        }

        /** Iterates every cell in the 7×7×7 shell, supplying its classification. The cursor is reused — copy via {@code immutable()} to retain. */
        public void forEachCell(BiConsumer<BlockPos, CellKind> action) {
            this.forEachCellUntil((pos, kind) -> {
                action.accept(pos, kind);
                return false;
            });
        }

        /** {@link #forEachCell} variant that stops as soon as {@code action} returns {@code true}. Returns whether iteration was stopped early. */
        public boolean forEachCellUntil(BiPredicate<BlockPos, CellKind> action) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = this.shellMin.getX(); x <= this.shellMax.getX(); x++) {
                for (int y = this.shellMin.getY(); y <= this.shellMax.getY(); y++) {
                    for (int z = this.shellMin.getZ(); z <= this.shellMax.getZ(); z++) {
                        cursor.set(x, y, z);
                        if (action.test(cursor, this.classify(x, y, z))) return true;
                    }
                }
            }
            return false;
        }

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
        return findInvalidPositions(layout, level, MAX_REPORTED_INVALID);
    }

    private static List<InvalidEntry> findInvalidPositions(Layout layout, LevelReader level, int limit) {
        List<InvalidEntry> out = new ArrayList<>();
        layout.forEachCellUntil((cursor, kind) -> {
            InvalidType problem = problemAt(level, cursor, kind);
            if (problem == null) return false;
            out.add(new InvalidEntry(cursor.immutable(), problem));
            return out.size() >= limit;
        });
        return out;
    }

    /** Returns the per-cell validation failure (or {@code null} when the cell satisfies its expected contents). */
    private static InvalidType problemAt(LevelReader level, BlockPos pos, CellKind kind) {
        if (kind == CellKind.CONTROLLER) return null;
        BlockState state = level.getBlockState(pos);
        return switch (kind) {
            case FLOOR -> state.is(Hostile.Tags.DATA_CENTER_FLOOR) ? null : InvalidType.OBSIDIAN_MISSING;
            case CEILING, WALL -> state.is(Hostile.Tags.DATA_CENTER_WALL) ? null : InvalidType.GLASS_MISSING;
            // Strict isAir() so replaceable plants/snow don't clip the central mob.
            case INTERIOR -> state.isAir() ? null : InvalidType.NOT_AIR;
            case CONTROLLER -> null;
        };
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
