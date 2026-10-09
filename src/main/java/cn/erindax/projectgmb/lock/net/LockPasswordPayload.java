package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockPasswordPayload(BlockPos pos, String password, boolean setup) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LockPasswordPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_password"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LockPasswordPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, LockPasswordPayload::pos,
		ByteBufCodecs.STRING_UTF8, LockPasswordPayload::password,
		ByteBufCodecs.BOOL, LockPasswordPayload::setup,
		LockPasswordPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
