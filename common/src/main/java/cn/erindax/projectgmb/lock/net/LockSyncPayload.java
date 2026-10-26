package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.lock.LockType;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record LockSyncPayload(List<Entry> entries) implements CustomPacketPayload {

	public record Entry(BlockPos pos, LockType type) {

		static Entry read(FriendlyByteBuf buf) {
			return new Entry(buf.readBlockPos(), LockType.byIndex(buf.readVarInt()));
		}

		void write(FriendlyByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeVarInt(type.ordinal());
		}
	}

	public static final CustomPacketPayload.Type<LockSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_sync"));

	public static final StreamCodec<FriendlyByteBuf, LockSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(LockSyncPayload::write, LockSyncPayload::new);

	private LockSyncPayload(FriendlyByteBuf buf) {
		this(buf.readList(Entry::read));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
