package net.neverandy.vr.client;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

/**
 * Reads the redstone around the player: how strong the signal is in each block, what repeaters and comparators are set
 * to, which pistons, dispensers and droppers are quasi-powered, and which powered blocks are wired to the block the
 * player looks at. The result is kept for a few ticks, so a large range does not slow every frame down.
 */
final class RedstoneScanner {

    /** Offsets for the six sides, in Minecraft's side order (down, up, north, south, west, east). */
    private static final int[] DX = { 0, 0, 0, 0, -1, 1 };
    private static final int[] DY = { -1, 1, 0, 0, 0, 0 };
    private static final int[] DZ = { 0, 0, -1, 1, 0, 0 };

    /** A block position, usable as a map key. */
    static final class Pos {

        final int x, y, z;

        Pos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Pos offset(int dx, int dy, int dz) {
            return new Pos(x + dx, y + dy, z + dz);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pos)) {
                return false;
            }
            Pos p = (Pos) o;
            return p.x == x && p.y == y && p.z == z;
        }

        @Override
        public int hashCode() {
            return (x * 31 + y) * 31 + z;
        }
    }

    /** What one block shows. */
    static final class Mark {

        final Pos pos;
        final int power;
        final boolean wire;
        /** Repeater delay, comparator mode or a quasi-connectivity hint; null when there is nothing to say. */
        final String info;
        final boolean qc;

        Mark(Pos pos, int power, boolean wire, String info, boolean qc) {
            this.pos = pos;
            this.power = power;
            this.wire = wire;
            this.info = info;
            this.qc = qc;
        }
    }

    static final class Result {

        static final Result EMPTY = new Result(Collections.<Pos, Mark>emptyMap(), Collections.<Pos>emptySet());

        final Map<Pos, Mark> marks;
        /** Powered blocks wired to the looked-at block, the looked-at block included. */
        final Set<Pos> path;

        Result(Map<Pos, Mark> marks, Set<Pos> path) {
            this.marks = marks;
            this.path = path;
        }
    }

    private static final int RESCAN_TICKS = 4;
    private static final int MAX_PATH = 512;

    private static World lastWorld;
    private static long lastTime;
    private static Pos lastCenter;
    private static Pos lastTarget;
    private static int lastRange;
    private static Result last = Result.EMPTY;

    private RedstoneScanner() {}

    static Result scan(World world, Pos center, Pos target, int range) {
        long time = world.getTotalWorldTime();
        if (world == lastWorld && center.equals(lastCenter)
            && range == lastRange
            && (target == null ? lastTarget == null : target.equals(lastTarget))
            && time >= lastTime
            && time - lastTime < RESCAN_TICKS) {
            return last;
        }
        lastWorld = world;
        lastTime = time;
        lastCenter = center;
        lastTarget = target;
        lastRange = range;

        Set<Pos> candidates = new HashSet<>();
        for (int x = center.x - range; x <= center.x + range; x++) {
            for (int y = Math.max(0, center.y - range); y <= Math.min(255, center.y + range); y++) {
                for (int z = center.z - range; z <= center.z + range; z++) {
                    Block block = world.getBlock(x, y, z);
                    if (block.getMaterial() != Material.air && isComponent(block)) {
                        Pos pos = new Pos(x, y, z);
                        candidates.add(pos);
                        // A component powers the blocks next to it, so those are worth a look too.
                        for (int side = 0; side < 6; side++) {
                            candidates.add(pos.offset(DX[side], DY[side], DZ[side]));
                        }
                    }
                }
            }
        }

        Map<Pos, Mark> marks = new LinkedHashMap<>();
        for (Pos pos : candidates) {
            if (Math.abs(pos.x - center.x) > range || Math.abs(pos.y - center.y) > range
                || Math.abs(pos.z - center.z) > range
                || !world.blockExists(pos.x, pos.y, pos.z)) {
                continue;
            }
            Mark mark = mark(world, pos);
            if (mark != null) {
                marks.put(pos, mark);
            }
        }

        last = new Result(marks, path(marks, target));
        return last;
    }

    private static boolean isComponent(Block block) {
        return block.canProvidePower() || block instanceof BlockRedstoneWire
            || block instanceof BlockPistonBase
            || block instanceof BlockDispenser;
    }

    private static Mark mark(World world, Pos pos) {
        Block block = world.getBlock(pos.x, pos.y, pos.z);
        if (block.getMaterial() == Material.air) {
            return null;
        }
        int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
        boolean wire = block instanceof BlockRedstoneWire;
        int power = power(world, pos.x, pos.y, pos.z);
        String info = null;
        boolean qc = false;
        if (block instanceof BlockRedstoneRepeater) {
            int delay = ((meta & 12) >> 2) + 1;
            info = delay + (delay == 1 ? " tick" : " ticks");
        } else if (block instanceof BlockRedstoneComparator) {
            info = (meta & 4) != 0 ? "subtract" : "compare";
        } else if (block instanceof BlockPistonBase || block instanceof BlockDispenser) {
            // Both keep "extended" or "triggered" in the top bit of their metadata.
            boolean active = (meta & 8) != 0;
            boolean direct = world.isBlockIndirectlyGettingPowered(pos.x, pos.y, pos.z);
            // Pistons, dispensers and droppers also count a signal reaching the block above them, but only notice it
            // when they get a block update.
            boolean above = world.isBlockIndirectlyGettingPowered(pos.x, pos.y + 1, pos.z);
            if (!active && !direct && above) {
                info = "QC: fires on next update";
                qc = true;
            } else if (active && !direct && !above && block instanceof BlockPistonBase) {
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
    static int power(World world, int x, int y, int z) {
        int power = world.getStrongestIndirectPower(x, y, z);
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockRedstoneWire) {
            power = Math.max(power, world.getBlockMetadata(x, y, z));
        }
        if (block.canProvidePower()) {
            for (int side = 0; side < 6; side++) {
                power = Math.max(power, block.isProvidingWeakPower(world, x, y, z, side));
            }
        }
        return power;
    }

    /** Walks from the looked-at block through every powered block touching it, and dust stepping up or down. */
    private static Set<Pos> path(Map<Pos, Mark> marks, Pos target) {
        Mark start = target == null ? null : marks.get(target);
        if (start == null || start.power <= 0) {
            return Collections.emptySet();
        }
        Set<Pos> seen = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        seen.add(start.pos);
        queue.add(start.pos);
        while (!queue.isEmpty() && seen.size() < MAX_PATH) {
            Pos pos = queue.poll();
            for (int side = 0; side < 6; side++) {
                visit(marks, pos.offset(DX[side], DY[side], DZ[side]), false, seen, queue);
            }
            if (marks.get(pos).wire) {
                for (int side = 2; side < 6; side++) {
                    visit(marks, pos.offset(DX[side], 1, DZ[side]), true, seen, queue);
                    visit(marks, pos.offset(DX[side], -1, DZ[side]), true, seen, queue);
                }
            }
        }
        return seen;
    }

    private static void visit(Map<Pos, Mark> marks, Pos pos, boolean wireOnly, Set<Pos> seen, ArrayDeque<Pos> queue) {
        Mark mark = marks.get(pos);
        if (mark != null && mark.power > 0 && (!wireOnly || mark.wire) && seen.add(pos)) {
            queue.add(pos);
        }
    }
}
