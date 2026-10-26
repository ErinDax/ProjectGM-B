package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.lock.DoorLockRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncDoorLocksPayload(List<Entry> entries) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncDoorLocksPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("sync_door_locks"));
	private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, Entry::pos,
		UUIDUtil.STREAM_CODEC, Entry::owner,
		Entry::new
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncDoorLocksPayload> CODEC = StreamCodec.composite(
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
