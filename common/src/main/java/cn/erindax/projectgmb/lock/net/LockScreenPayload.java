package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LockScreenPayload(BlockPos pos, Kind kind, int lockType, int mode) implements CustomPacketPayload {

	public enum Kind {
		SETUP_PASSWORD, UNLOCK_PASSWORD, OWNER
	}

	public static final CustomPacketPayload.Type<LockScreenPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("lock_screen"));

	public static final StreamCodec<RegistryFriendlyByteBuf, LockScreenPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, LockScreenPayload::pos,
		ByteBufCodecs.VAR_INT.map(i -> Kind.values()[i], Kind::ordinal), LockScreenPayload::kind,
		ByteBufCodecs.VAR_INT, LockScreenPayload::lockType,
		ByteBufCodecs.VAR_INT, LockScreenPayload::mode,
		LockScreenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
