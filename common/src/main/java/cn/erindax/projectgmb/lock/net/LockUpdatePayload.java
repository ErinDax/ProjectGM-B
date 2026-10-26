package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LockUpdatePayload(BlockPos pos, int lockType) implements CustomPacketPayload {

	public static final int REMOVED = -1;

	public static final CustomPacketPayload.Type<LockUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_update"));

	public static final StreamCodec<FriendlyByteBuf, LockUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BLOCK_POS, LockUpdatePayload::pos,
		ByteBufCodecs.INT, LockUpdatePayload::lockType,
		LockUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
