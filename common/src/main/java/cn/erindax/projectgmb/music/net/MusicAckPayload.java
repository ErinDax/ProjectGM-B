package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record MusicAckPayload(String track, int version, int index) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicAckPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_ack"));

	public static final StreamCodec<FriendlyByteBuf, MusicAckPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, MusicAckPayload::track,
		ByteBufCodecs.INT, MusicAckPayload::version,
		ByteBufCodecs.VAR_INT, MusicAckPayload::index,
		MusicAckPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
