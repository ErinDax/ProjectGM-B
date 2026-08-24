package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoorLockSavedData extends SavedData {
	private final Map<BlockPos, DoorLockRecord> locks = new HashMap<>();

	public static DoorLockSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(
			new SavedData.Factory<>(DoorLockSavedData::new, DoorLockSavedData::load, DataFixTypes.LEVEL),
			"projectgm_b_door_locks"
		);
	}

	public static DoorLockSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		DoorLockSavedData data = new DoorLockSavedData();
		ListTag list = tag.getList("Locks", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			BlockPos pos = BlockPos.of(entry.getLong("Pos"));
			UUID owner = entry.getUUID("Owner");
			data.locks.put(pos, new DoorLockRecord(owner));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		locks.forEach((pos, record) -> {
			CompoundTag entry = new CompoundTag();
			entry.putLong("Pos", pos.asLong());
			entry.putUUID("Owner", record.owner());
			list.add(entry);
		});
		tag.put("Locks", list);
		return tag;
	}

	public DoorLockRecord get(BlockPos pos) {
		return locks.get(pos.immutable());
	}

	public void put(BlockPos pos, DoorLockRecord record) {
		locks.put(pos.immutable(), record);
		setDirty();
	}

	public void remove(BlockPos pos) {
		if (locks.remove(pos.immutable()) != null) {
			setDirty();
		}
	}

	public Map<BlockPos, DoorLockRecord> snapshot() {
		return Map.copyOf(locks);
	}

	public void sync(ServerLevel level) {
		SyncDoorLocksPayload payload = SyncDoorLocksPayload.of(locks);
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
