package de.christmasland;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Teleportiert Spieler, die lange genug im Eisportal stehen. */
public final class PortalTravel {
	private static final int WAIT_TICKS = 80;
	private static final int COOLDOWN_TICKS = 100;

	private static final Map<UUID, Integer> WAITING = new HashMap<>();
	private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();

	private PortalTravel() {
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			ServerLevel level = (ServerLevel) player.level();

			BlockPos portalPos = null;
			BlockPos feet = player.blockPosition();
			if (PortalFrames.isPortal(level.getBlockState(feet))) {
				portalPos = feet;
			} else if (PortalFrames.isPortal(level.getBlockState(feet.above()))) {
				portalPos = feet.above();
			}

			int cooldown = COOLDOWN.getOrDefault(id, 0);

			if (portalPos == null) {
				WAITING.remove(id);
				if (cooldown > 0) {
					COOLDOWN.put(id, cooldown - 1);
				}
				continue;
			}

			// Portal kaputt (Rahmen zerstoert)? Dann entfernen.
			if (!PortalFrames.intact(level, portalPos)) {
				PortalFrames.removeConnected(level, portalPos);
				WAITING.remove(id);
				continue;
			}

			if (cooldown > 0) {
				COOLDOWN.put(id, COOLDOWN_TICKS);
				continue;
			}

			int waited = WAITING.getOrDefault(id, 0) + 1;
			int needed = player.isCreative() ? 1 : WAIT_TICKS;
			if (waited >= needed) {
				WAITING.remove(id);
				travel(server, player, level, portalPos);
			} else {
				WAITING.put(id, waited);
			}
		}
	}

	private static void travel(MinecraftServer server, ServerPlayer player, ServerLevel from, BlockPos portalPos) {
		ServerLevel target = from.dimension().equals(ChristmaslandMod.CHRISTMASLAND)
				? server.overworld()
				: server.getLevel(ChristmaslandMod.CHRISTMASLAND);
		if (target == null) {
			return;
		}

		BlockPos dest = PortalFrames.findOrBuild(target, portalPos.getX(), portalPos.getZ());
		Vec3 pos = Vec3.atBottomCenterOf(dest);

		COOLDOWN.put(player.getUUID(), COOLDOWN_TICKS);
		player.teleport(new TeleportTransition(
				target, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
	}
}
