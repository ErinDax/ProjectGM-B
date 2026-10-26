package cn.erindax.projectgmb.entity.ai;

import cn.erindax.projectgmb.entity.PatrollerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;
import java.util.List;

public class PatrolRouteGoal extends Goal {

	private static final double ARRIVE_DIST_SQR = 1.0;
	private static final int REPATH_INTERVAL = 20;
	private static final int STUCK_LIMIT = 15 * 20;

	private final PatrollerEntity mob;
	private final double speed;

	private int pauseLeft;
	private int repathCooldown;
	private int stuckTicks;

	public PatrolRouteGoal(PatrollerEntity mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return mob.isPatrolling() && mob.getState() == PatrollerEntity.State.PATROL && !mob.getWaypoints().isEmpty();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		repathCooldown = 0;
		stuckTicks = 0;
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		List<BlockPos> route = mob.getWaypoints();
		int index = Math.min(mob.getRouteIndex(), route.size() - 1);
		BlockPos target = route.get(index);
		double x = target.getX() + 0.5;
		double y = target.getY();
		double z = target.getZ() + 0.5;

		if (pauseLeft > 0) {
			pauseLeft--;
			mob.getNavigation().stop();
			return;
		}

		double dx = mob.getX() - x;
		double dz = mob.getZ() - z;
		double dy = mob.getY() - y;
		if (dx * dx + dz * dz <= ARRIVE_DIST_SQR && Math.abs(dy) <= 2.0) {
			advance(route.size());
			return;
		}

		if (--repathCooldown <= 0) {
			repathCooldown = REPATH_INTERVAL;
			Path path = mob.getNavigation().createPath(target, 0);
			if (mob.getNavigation().moveTo(path, speed) && path.canReach()) {
				stuckTicks = 0;
			} else {
				stuckTicks += REPATH_INTERVAL;
			}
		} else if (mob.getNavigation().isDone()) {
			stuckTicks++;
		}

		if (stuckTicks >= STUCK_LIMIT) {
			stuckTicks = 0;
			advance(route.size());
		}
	}

	private void advance(int size) {
		stuckTicks = 0;
		repathCooldown = 0;
		if (size <= 1) {
			mob.getNavigation().stop();
			return;
		}
		int index = mob.getRouteIndex();
		if (mob.isRouteForward()) {
			if (index >= size - 1) {
				mob.setRouteForward(false);
				mob.setRouteIndex(size - 2);
			} else {
				mob.setRouteIndex(index + 1);
			}
		} else {
			if (index <= 0) {
				mob.setRouteForward(true);
				mob.setRouteIndex(1);
				pauseLeft = mob.getPauseTicks();
			} else {
				mob.setRouteIndex(index - 1);
			}
		}
	}
}
