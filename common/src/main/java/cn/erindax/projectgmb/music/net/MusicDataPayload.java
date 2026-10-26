package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record MusicDataPayload(String track, int version, int index, int total, byte[] data)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicDataPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_data"));

	public static final StreamCodec<FriendlyByteBuf, MusicDataPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, MusicDataPayload::track,
		ByteBufCodecs.INT, MusicDataPayload::version,
		ByteBufCodecs.VAR_INT, MusicDataPayload::index,
		ByteBufCodecs.VAR_INT, MusicDataPayload::total,
		ByteBufCodecs.BYTE_ARRAY, MusicDataPayload::data,
		MusicDataPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
