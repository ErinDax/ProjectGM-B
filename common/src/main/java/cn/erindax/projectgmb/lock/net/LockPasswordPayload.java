package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record LockPasswordPayload(BlockPos pos, String password, boolean setup) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<LockPasswordPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_password"));

	public static final StreamCodec<FriendlyByteBuf, LockPasswordPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BLOCK_POS, LockPasswordPayload::pos,
		ByteBufCodecs.STRING_UTF8, LockPasswordPayload::password,
		ByteBufCodecs.BOOL, LockPasswordPayload::setup,
		LockPasswordPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
