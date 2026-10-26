package cn.erindax.projectgmb.dance.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.dance.DanceStats;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

public record DanceProgressPayload(int session, DanceStats stats, int state) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DanceProgressPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("dance_progress"));

	public static final StreamCodec<ByteBuf, DanceProgressPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceProgressPayload::session,
		DanceStats.STREAM_CODEC, DanceProgressPayload::stats,
		ByteBufCodecs.VAR_INT, DanceProgressPayload::state,
		DanceProgressPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
