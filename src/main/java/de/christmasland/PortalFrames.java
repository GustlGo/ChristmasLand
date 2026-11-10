package de.christmasland;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Erkennen, Entzuenden, Entfernen und Bauen von Eisportalen. */
public final class PortalFrames {
	private static final int MAX = 21;

	private PortalFrames() {
	}

	/** Eis, Packeis und Blaueis sind erlaubte Rahmenbloecke. */
	public static boolean isFrame(BlockState state) {
		return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
	}

	public static boolean isPortal(BlockState state) {
		return state.is(ChristmaslandMod.ICE_PORTAL);
	}

	private static Direction positive(Direction.Axis axis) {
		return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
	}

	/** Versucht an der Position (Luft innerhalb des Rahmens) ein Portal zu zuenden. */
	public static boolean tryLight(Level level, BlockPos start) {
		for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z }) {
			if (lightAxis(level, start, axis)) {
				return true;
			}
		}
		return false;
	}

	private static boolean lightAxis(Level level, BlockPos start, Direction.Axis axis) {
		if (!level.getBlockState(start).isAir()) {
			return false;
		}
		Direction right = positive(axis);
		Direction left = right.getOpposite();

		// nach unten bis zum Rahmenboden
		BlockPos p = start;
		int down = 0;
		while (level.getBlockState(p.below()).isAir() && down < MAX) {
			p = p.below();
			down++;
		}
		if (!isFrame(level.getBlockState(p.below()))) {
			return false;
		}

		// nach links bis zur linken Rahmenseite
		int steps = 0;
		while (steps < MAX
				&& level.getBlockState(p.relative(left)).isAir()
				&& isFrame(level.getBlockState(p.relative(left).below()))) {
			p = p.relative(left);
			steps++;
		}
		if (!isFrame(level.getBlockState(p.relative(left)))) {
			return false;
		}

		// Breite bestimmen
		int width = 1;
		while (width < MAX
				&& level.getBlockState(p.relative(right, width)).isAir()
				&& isFrame(level.getBlockState(p.relative(right, width).below()))) {
			width++;
		}
		if (width < 2 || !isFrame(level.getBlockState(p.relative(right, width)))) {
			return false;
		}

		// Hoehe bestimmen
		int height = 0;
		while (height < MAX && rowOk(level, p, right, width, height)) {
			height++;
		}
		if (height < 3) {
			return false;
		}
		for (int k = 0; k < width; k++) {
			if (!isFrame(level.getBlockState(p.relative(right, k).above(height)))) {
				return false;
			}
		}

		// Portalbloecke setzen
		BlockState portal = ChristmaslandMod.ICE_PORTAL.defaultBlockState().setValue(IcePortalBlock.AXIS, axis);
		for (int k = 0; k < width; k++) {
			for (int y = 0; y < height; y++) {
				level.setBlock(p.relative(right, k).above(y), portal, Block.UPDATE_ALL);
			}
		}
		return true;
	}

	private static boolean rowOk(Level level, BlockPos origin, Direction right, int width, int y) {
		if (!isFrame(level.getBlockState(origin.relative(right.getOpposite()).above(y)))) {
			return false;
		}
		if (!isFrame(level.getBlockState(origin.relative(right, width).above(y)))) {
			return false;
		}
		for (int k = 0; k < width; k++) {
			if (!level.getBlockState(origin.relative(right, k).above(y)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Entfernt alle zusammenhaengenden Portalbloecke rund um eine Position. */
	public static void removeConnected(Level level, BlockPos origin) {
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		int removed = 0;
		while (!queue.isEmpty() && removed < 600) {
			BlockPos pos = queue.poll();
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (isPortal(level.getBlockState(next))) {
					level.setBlock(next, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					removed++;
					queue.add(next);
				}
			}
		}
	}

	/** Prueft, ob ein Portalblock noch von Portal/Rahmen umgeben ist. */
	public static boolean intact(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!isPortal(state)) {
			return false;
		}
		Direction h = positive(state.getValue(IcePortalBlock.AXIS));
		for (Direction d : new Direction[] { Direction.UP, Direction.DOWN, h, h.getOpposite() }) {
			BlockState n = level.getBlockState(pos.relative(d));
			if (!isPortal(n) && !isFrame(n)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sucht in der Zieldimension ein Portal nahe (x, z) oder baut ein neues.
	 * Gibt die Position eines Portalblocks zurueck.
	 */
	public static BlockPos findOrBuild(ServerLevel level, int x, int z) {
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

		for (BlockPos pos : BlockPos.betweenClosed(x - 16, y - 8, z - 16, x + 16, y + 8, z + 16)) {
			if (isPortal(level.getBlockState(pos)) && isFrame(level.getBlockState(pos.below()))) {
				return pos.immutable();
			}
		}
		return build(level, x, y, z);
	}

	/** Baut ein 2x3-Portal (Achse X) mit Blaueis-Rahmen und Packeis-Plattform. */
	private static BlockPos build(ServerLevel level, int x, int y, int z) {
		BlockState blue = Blocks.BLUE_ICE.defaultBlockState();
		BlockState platform = Blocks.PACKED_ICE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState portal = ChristmaslandMod.ICE_PORTAL.defaultBlockState().setValue(IcePortalBlock.AXIS, Direction.Axis.X);

		// Plattform und freier Raum
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), platform, Block.UPDATE_ALL);
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlock(new BlockPos(x + dx, y + dy, z + dz), air, Block.UPDATE_ALL);
				}
			}
		}
		// Rahmen: x-1..x+2, y-1..y+3
		for (int dx = -1; dx <= 2; dx++) {
			for (int dy = -1; dy <= 3; dy++) {
				boolean edge = dx == -1 || dx == 2 || dy == -1 || dy == 3;
				if (edge) {
					level.setBlock(new BlockPos(x + dx, y + dy, z), blue, Block.UPDATE_ALL);
				}
			}
		}
		// Portal innen: x..x+1, y..y+2
		for (int dx = 0; dx <= 1; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z), portal, Block.UPDATE_ALL);
			}
		}
		return new BlockPos(x, y, z);
	}
}
