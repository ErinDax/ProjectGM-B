package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.lock.LockType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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

	public static final StreamCodec<RegistryFriendlyByteBuf, LockSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(LockSyncPayload::write, LockSyncPayload::new);

	private LockSyncPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(Entry::read));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
