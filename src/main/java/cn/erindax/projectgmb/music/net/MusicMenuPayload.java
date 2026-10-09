package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record MusicMenuPayload(boolean mainHand, String track, int range, boolean playing, boolean paused,
		List<String> tracks) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicMenuPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_menu"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicMenuPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MusicMenuPayload::write, MusicMenuPayload::new);

	private MusicMenuPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
			buf.readList(FriendlyByteBuf::readUtf));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(mainHand);
		buf.writeUtf(track);
		buf.writeVarInt(range);
		buf.writeBoolean(playing);
		buf.writeBoolean(paused);
		buf.writeCollection(tracks, FriendlyByteBuf::writeUtf);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
