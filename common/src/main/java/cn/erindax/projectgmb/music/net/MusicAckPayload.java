package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MusicAckPayload(String track, int version, int index) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicAckPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_ack"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicAckPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, MusicAckPayload::track,
		ByteBufCodecs.INT, MusicAckPayload::version,
		ByteBufCodecs.VAR_INT, MusicAckPayload::index,
		MusicAckPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
