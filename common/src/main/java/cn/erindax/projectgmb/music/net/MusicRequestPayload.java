package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record MusicRequestPayload(String track) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_request"));

	public static final StreamCodec<FriendlyByteBuf, MusicRequestPayload> STREAM_CODEC =
		StreamCodec.composite(ByteBufCodecs.STRING_UTF8, MusicRequestPayload::track, MusicRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
