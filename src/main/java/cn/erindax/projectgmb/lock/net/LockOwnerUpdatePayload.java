package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockOwnerUpdatePayload(BlockPos pos, int mode, boolean remove) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LockOwnerUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_owner_update"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LockOwnerUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, LockOwnerUpdatePayload::pos,
		ByteBufCodecs.VAR_INT, LockOwnerUpdatePayload::mode,
		ByteBufCodecs.BOOL, LockOwnerUpdatePayload::remove,
		LockOwnerUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
