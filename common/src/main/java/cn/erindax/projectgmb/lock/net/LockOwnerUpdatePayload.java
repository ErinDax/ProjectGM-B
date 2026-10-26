package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LockOwnerUpdatePayload(BlockPos pos, int mode, boolean remove) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LockOwnerUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_owner_update"));

	public static final StreamCodec<FriendlyByteBuf, LockOwnerUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BLOCK_POS, LockOwnerUpdatePayload::pos,
		ByteBufCodecs.VAR_INT, LockOwnerUpdatePayload::mode,
		ByteBufCodecs.BOOL, LockOwnerUpdatePayload::remove,
		LockOwnerUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
