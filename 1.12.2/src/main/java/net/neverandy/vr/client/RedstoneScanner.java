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
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reads the redstone around the player: how strong the signal is in each block, what repeaters and comparators are set
 * to, which pistons, dispensers and droppers are quasi-powered, and which powered blocks are wired to the block the
 * player looks at. The result is kept for a few ticks, so a large range does not slow every frame down.
 */
final class RedstoneScanner
{
	/** What one block shows. */
	static final class Mark
	{
		final BlockPos pos;
		final int power;
		final boolean wire;
		/** Repeater delay, comparator mode or a quasi-connectivity hint; null when there is nothing to say. */
		final String info;
		final boolean qc;

		Mark(BlockPos pos, int power, boolean wire, String info, boolean qc)
		{
			this.pos = pos;
			this.power = power;
			this.wire = wire;
			this.info = info;
			this.qc = qc;
		}
	}

	static final class Result
	{
		static final Result EMPTY = new Result(Collections.<BlockPos, Mark>emptyMap(), Collections.<BlockPos>emptySet());

		final Map<BlockPos, Mark> marks;
		/** Powered blocks wired to the looked-at block, the looked-at block included. */
		final Set<BlockPos> path;

		Result(Map<BlockPos, Mark> marks, Set<BlockPos> path)
		{
			this.marks = marks;
			this.path = path;
		}
	}

	private static final int RESCAN_TICKS = 4;
	private static final int MAX_PATH = 512;

	private static World lastWorld;
	private static long lastTime;
	private static BlockPos lastCenter;
	private static BlockPos lastTarget;
	private static int lastRange;
	private static Result last = Result.EMPTY;

	private RedstoneScanner()
	{
	}

	static Result scan(World world, BlockPos center, BlockPos target, int range)
	{
		long time = world.getTotalWorldTime();
		if (world == lastWorld && center.equals(lastCenter) && range == lastRange
				&& (target == null ? lastTarget == null : target.equals(lastTarget))
				&& time >= lastTime && time - lastTime < RESCAN_TICKS)
		{
			return last;
		}
		lastWorld = world;
		lastTime = time;
		lastCenter = center.toImmutable();
		lastTarget = target == null ? null : target.toImmutable();
		lastRange = range;

		Set<BlockPos> candidates = new HashSet<>();
		for (BlockPos p : BlockPos.getAllInBoxMutable(center.add(-range, -range, -range), center.add(range, range, range)))
		{
			IBlockState state = world.getBlockState(p);
			if (state.getMaterial() != Material.AIR && isComponent(state))
			{
				BlockPos pos = p.toImmutable();
				candidates.add(pos);
				// A component powers the blocks next to it, so those are worth a look too.
				for (EnumFacing facing : EnumFacing.values())
				{
					candidates.add(pos.offset(facing));
				}
			}
		}

		Map<BlockPos, Mark> marks = new LinkedHashMap<>();
		for (BlockPos pos : candidates)
		{
			if (Math.abs(pos.getX() - center.getX()) > range || Math.abs(pos.getY() - center.getY()) > range
					|| Math.abs(pos.getZ() - center.getZ()) > range || !world.isBlockLoaded(pos))
			{
				continue;
			}
			Mark mark = mark(world, pos, world.getBlockState(pos));
			if (mark != null)
			{
				marks.put(pos, mark);
			}
		}

		last = new Result(marks, path(marks, target));
		return last;
	}

	private static boolean isComponent(IBlockState state)
	{
		Block block = state.getBlock();
		return state.canProvidePower() || block instanceof BlockRedstoneWire || block instanceof BlockPistonBase
				|| block instanceof BlockDispenser;
	}

	private static Mark mark(World world, BlockPos pos, IBlockState state)
	{
		if (state.getMaterial() == Material.AIR)
		{
			return null;
		}
		Block block = state.getBlock();
		boolean wire = block instanceof BlockRedstoneWire;
		int power = power(world, pos, state);
		String info = null;
		boolean qc = false;
		if (block instanceof BlockRedstoneRepeater)
		{
			// Whether a repeater is locked is worked out from its neighbors, not stored.
			IBlockState actual = state.getActualState(world, pos);
			int delay = actual.getValue(BlockRedstoneRepeater.DELAY);
			info = delay + (delay == 1 ? " tick" : " ticks") + (actual.getValue(BlockRedstoneRepeater.LOCKED) ? ", locked" : "");
		}
		else if (block instanceof BlockRedstoneComparator)
		{
			info = state.getValue(BlockRedstoneComparator.MODE) == BlockRedstoneComparator.Mode.SUBTRACT ? "subtract" : "compare";
		}
		else if (block instanceof BlockPistonBase || block instanceof BlockDispenser)
		{
			boolean active = block instanceof BlockPistonBase ? state.getValue(BlockPistonBase.EXTENDED)
					: state.getValue(BlockDispenser.TRIGGERED);
			boolean direct = world.isBlockPowered(pos);
			// Pistons, dispensers and droppers also count a signal reaching the block above them, but only notice it
			// when they get a block update.
			boolean above = world.isBlockPowered(pos.up());
			if (!active && !direct && above)
			{
				info = "QC: fires on next update";
				qc = true;
			}
			else if (active && !direct && !above && block instanceof BlockPistonBase)
			{
				info = "QC: retracts on next update";
				qc = true;
			}
		}
		if (!wire && power <= 0 && info == null)
		{
			return null;
		}
		return new Mark(pos, power, wire, info, qc);
	}

	/** The strongest redstone signal reaching the block, the power level of redstone dust, or what a source puts out. */
	static int power(World world, BlockPos pos, IBlockState state)
	{
		int power = world.isBlockIndirectlyGettingPowered(pos);
		if (state.getBlock() instanceof BlockRedstoneWire)
		{
			power = Math.max(power, state.getValue(BlockRedstoneWire.POWER));
		}
		if (state.canProvidePower())
		{
			for (EnumFacing facing : EnumFacing.values())
			{
				power = Math.max(power, state.getWeakPower(world, pos, facing));
			}
		}
		return power;
	}

	/** Walks from the looked-at block through every powered block touching it, and dust stepping up or down. */
	private static Set<BlockPos> path(Map<BlockPos, Mark> marks, BlockPos target)
	{
		Mark start = target == null ? null : marks.get(target);
		if (start == null || start.power <= 0)
		{
			return Collections.emptySet();
		}
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start.pos);
		queue.add(start.pos);
		while (!queue.isEmpty() && seen.size() < MAX_PATH)
		{
			BlockPos pos = queue.poll();
			for (EnumFacing facing : EnumFacing.values())
			{
				visit(marks, pos.offset(facing), false, seen, queue);
			}
			if (marks.get(pos).wire)
			{
				for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL)
				{
					visit(marks, pos.offset(facing).up(), true, seen, queue);
					visit(marks, pos.offset(facing).down(), true, seen, queue);
				}
			}
		}
		return seen;
	}

	private static void visit(Map<BlockPos, Mark> marks, BlockPos pos, boolean wireOnly, Set<BlockPos> seen,
			ArrayDeque<BlockPos> queue)
	{
		Mark mark = marks.get(pos);
		if (mark != null && mark.power > 0 && (!wireOnly || mark.wire) && seen.add(pos))
		{
			queue.add(pos);
		}
	}
}
