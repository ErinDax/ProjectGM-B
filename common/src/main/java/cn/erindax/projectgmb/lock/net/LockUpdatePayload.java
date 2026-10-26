package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockUpdatePayload(BlockPos pos, int lockType) implements CustomPacketPayload {

	public static final int REMOVED = -1;

	public static final CustomPacketPayload.Type<LockUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_update"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LockUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, LockUpdatePayload::pos,
		ByteBufCodecs.INT, LockUpdatePayload::lockType,
		LockUpdatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
