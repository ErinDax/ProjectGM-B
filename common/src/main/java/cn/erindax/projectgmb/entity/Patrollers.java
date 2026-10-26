package cn.erindax.projectgmb.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Patrollers {

	public static final int MAX_NAME_LENGTH = 32;

	private Patrollers() {
	}

	public static boolean isValidName(String name) {
		return !name.isEmpty() && name.length() <= MAX_NAME_LENGTH;
	}

	public static List<PatrollerEntity> all(MinecraftServer server) {
		List<PatrollerEntity> result = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			result.addAll(level.getEntities(ModEntities.PATROLLER, p -> !p.getPatrolName().isEmpty()));
		}
		return result;
	}

	@Nullable
	public static PatrollerEntity findByName(MinecraftServer server, String name) {
		for (ServerLevel level : server.getAllLevels()) {
			List<? extends PatrollerEntity> found = level.getEntities(ModEntities.PATROLLER,
				p -> name.equals(p.getPatrolName()));
			if (!found.isEmpty()) {
				return found.get(0);
			}
		}
		return null;
	}

	@Nullable
	public static PatrollerEntity spawn(ServerLevel level, String name, List<BlockPos> route, float yaw) {
		BlockPos start = route.get(0);
		PatrollerEntity patroller = ModEntities.PATROLLER.create(level);
		if (patroller == null) {
			return null;
		}
		patroller.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, yaw, 0.0F);
		patroller.setPatrolName(name);
		patroller.setWaypoints(route);
		patroller.finalizeSpawn(level, level.getCurrentDifficultyAt(start), MobSpawnType.COMMAND, null);
		level.addFreshEntity(patroller);
		return patroller;
	}
}
