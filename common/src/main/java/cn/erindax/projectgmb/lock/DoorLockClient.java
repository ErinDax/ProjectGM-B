package cn.erindax.projectgmb.lock;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class DoorLockClient {
	private static final Map<BlockPos, DoorLockRecord> LOCKS = new HashMap<>();

	private DoorLockClient() {
	}

	public static DoorLockRecord get(BlockPos pos) {
		return LOCKS.get(pos.immutable());
	}

	public static void replace(Map<BlockPos, DoorLockRecord> locks) {
		LOCKS.clear();
		LOCKS.putAll(locks);
	}
}
