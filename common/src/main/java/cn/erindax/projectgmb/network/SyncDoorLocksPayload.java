package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.lock.DoorLockRecord;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncDoorLocksPayload(List<Entry> entries) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncDoorLocksPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("sync_door_locks"));
	private static final StreamCodec<FriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
		ByteBufCodecs.BLOCK_POS, Entry::pos,
		ByteBufCodecs.UUID, Entry::owner,
		Entry::new
	);
	public static final StreamCodec<FriendlyByteBuf, SyncDoorLocksPayload> CODEC = StreamCodec.composite(
		ENTRY_CODEC.apply(ByteBufCodecs.list()),
		SyncDoorLocksPayload::entries,
		SyncDoorLocksPayload::new
	);

	@Override
	public CustomPacketPayload.Type<SyncDoorLocksPayload> type() {
		return TYPE;
	}

	public Map<BlockPos, DoorLockRecord> toMap() {
		Map<BlockPos, DoorLockRecord> map = new HashMap<>();
		for (Entry entry : entries) {
			map.put(entry.pos(), new DoorLockRecord(entry.owner()));
		}
		return map;
	}

	public static SyncDoorLocksPayload of(Map<BlockPos, DoorLockRecord> locks) {
		List<Entry> entries = new ArrayList<>(locks.size());
		locks.forEach((pos, record) -> entries.add(new Entry(pos, record.owner())));
		return new SyncDoorLocksPayload(entries);
	}

	public record Entry(BlockPos pos, UUID owner) {
	}
}
