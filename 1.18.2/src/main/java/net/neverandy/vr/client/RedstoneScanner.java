package net.neverandy.vr.client;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

/**
 * Reads the redstone around the player: how strong the signal is in each block, what repeaters and comparators are set
 * to, which pistons, dispensers and droppers are quasi-powered, and which powered blocks are wired to the block the
 * player looks at. The result is kept for a few ticks, so a large range does not slow every frame down.
 */
final class RedstoneScanner {

    /** What one block shows. */
    static final class Mark {
        final BlockPos pos;
        final int power;
        final boolean wire;
        /** Repeater delay, comparator mode or a quasi-connectivity hint; null when there is nothing to say. */
        final String info;
        final boolean qc;

        Mark(BlockPos pos, int power, boolean wire, String info, boolean qc) {
            this.pos = pos;
            this.power = power;
            this.wire = wire;
            this.info = info;
            this.qc = qc;
        }
    }

    static final class Result {
        static final Result EMPTY = new Result(Collections.emptyMap(), Collections.emptySet());

        final Map<BlockPos, Mark> marks;
        /** Powered blocks wired to the looked-at block, the looked-at block included. */
        final Set<BlockPos> path;

        Result(Map<BlockPos, Mark> marks, Set<BlockPos> path) {
            this.marks = marks;
            this.path = path;
        }
    }

    private static final int RESCAN_TICKS = 4;
    private static final int MAX_PATH = 512;

    private static Level lastLevel;
    private static long lastTime;
    private static BlockPos lastCenter;
    private static BlockPos lastTarget;
    private static int lastRange;
    private static Result last = Result.EMPTY;

    private RedstoneScanner() {
    }

    static Result scan(Level level, BlockPos center, BlockPos target, int range) {
        long time = level.getGameTime();
        if (level == lastLevel && center.equals(lastCenter) && range == lastRange
            && (target == null ? lastTarget == null : target.equals(lastTarget))
            && time >= lastTime && time - lastTime < RESCAN_TICKS) {
            return last;
        }
        lastLevel = level;
        lastTime = time;
        lastCenter = center.immutable();
        lastTarget = target == null ? null : target.immutable();
        lastRange = range;

        Set<BlockPos> candidates = new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && isComponent(state)) {
                BlockPos pos = p.immutable();
                candidates.add(pos);
                // A component powers the blocks next to it, so those are worth a look too.
                for (Direction d : Direction.values()) {
                    candidates.add(pos.relative(d));
                }
            }
        }

        Map<BlockPos, Mark> marks = new LinkedHashMap<>();
        for (BlockPos pos : candidates) {
            if (Math.abs(pos.getX() - center.getX()) > range || Math.abs(pos.getY() - center.getY()) > range
                || Math.abs(pos.getZ() - center.getZ()) > range || !level.isLoaded(pos)) {
                continue;
            }
            Mark mark = mark(level, pos, level.getBlockState(pos));
            if (mark != null) {
                marks.put(pos, mark);
            }
        }

        last = new Result(marks, path(marks, target));
        return last;
    }

    private static boolean isComponent(BlockState state) {
        Block block = state.getBlock();
        return state.isSignalSource() || block instanceof RedStoneWireBlock || block instanceof PistonBaseBlock
            || block instanceof DispenserBlock;
    }

    private static Mark mark(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return null;
        }
        Block block = state.getBlock();
        boolean wire = block instanceof RedStoneWireBlock;
        int power = power(level, pos, state);
        String info = null;
        boolean qc = false;
        if (block instanceof RepeaterBlock) {
            int delay = state.getValue(RepeaterBlock.DELAY);
            info = delay + (delay == 1 ? " tick" : " ticks") + (state.getValue(RepeaterBlock.LOCKED) ? ", locked" : "");
        } else if (block instanceof ComparatorBlock) {
            info = state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT ? "subtract" : "compare";
        } else if (block instanceof PistonBaseBlock || block instanceof DispenserBlock) {
            boolean active = block instanceof PistonBaseBlock ? state.getValue(PistonBaseBlock.EXTENDED)
                : state.getValue(DispenserBlock.TRIGGERED);
            boolean direct = level.hasNeighborSignal(pos);
            // Pistons, dispensers and droppers also count a signal reaching the block above them, but only notice it
            // when they get a block update.
            boolean above = level.hasNeighborSignal(pos.above());
            if (!active && !direct && above) {
                info = "QC: fires on next update";
                qc = true;
            } else if (active && !direct && !above && block instanceof PistonBaseBlock) {
                info = "QC: retracts on next update";
                qc = true;
            }
        }
        if (!wire && power <= 0 && info == null) {
            return null;
        }
        return new Mark(pos, power, wire, info, qc);
    }

    /** The strongest redstone signal reaching the block, the power level of redstone dust, or what a source puts out. */
    static int power(Level level, BlockPos pos, BlockState state) {
        int power = level.getBestNeighborSignal(pos);
        if (state.getBlock() instanceof RedStoneWireBlock) {
            power = Math.max(power, state.getValue(RedStoneWireBlock.POWER));
        }
        if (state.isSignalSource()) {
            for (Direction d : Direction.values()) {
                power = Math.max(power, state.getSignal(level, pos, d));
            }
        }
        return power;
    }

    /** Walks from the looked-at block through every powered block touching it, and dust stepping up or down. */
    private static Set<BlockPos> path(Map<BlockPos, Mark> marks, BlockPos target) {
        Mark start = target == null ? null : marks.get(target);
        if (start == null || start.power <= 0) {
            return Collections.emptySet();
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start.pos);
        queue.add(start.pos);
        while (!queue.isEmpty() && seen.size() < MAX_PATH) {
            BlockPos pos = queue.poll();
            for (Direction d : Direction.values()) {
                visit(marks, pos.relative(d), false, seen, queue);
            }
            if (marks.get(pos).wire) {
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    visit(marks, pos.relative(d).above(), true, seen, queue);
                    visit(marks, pos.relative(d).below(), true, seen, queue);
                }
            }
        }
        return seen;
    }

    private static void visit(Map<BlockPos, Mark> marks, BlockPos pos, boolean wireOnly, Set<BlockPos> seen,
                              ArrayDeque<BlockPos> queue) {
        Mark mark = marks.get(pos);
        if (mark != null && mark.power > 0 && (!wireOnly || mark.wire) && seen.add(pos)) {
            queue.add(pos);
        }
    }
}
