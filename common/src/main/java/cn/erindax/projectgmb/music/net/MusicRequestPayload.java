package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MusicRequestPayload(String track) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicRequestPayload> STREAM_CODEC =
		StreamCodec.composite(ByteBufCodecs.STRING_UTF8, MusicRequestPayload::track, MusicRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
