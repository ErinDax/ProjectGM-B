package cn.erindax.projectgmb.dance.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

public record DanceSignalPayload(int session, int signal) implements CustomPacketPayload {

	public static final int READY = 0;
	public static final int GO = 1;
	public static final int CLOSE = 2;

	public static final CustomPacketPayload.Type<DanceSignalPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("dance_signal"));

	public static final StreamCodec<ByteBuf, DanceSignalPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceSignalPayload::session,
		ByteBufCodecs.VAR_INT, DanceSignalPayload::signal,
		DanceSignalPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
