package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MusicNoteActionPayload(boolean mainHand, Action action, String track, int range)
		implements CustomPacketPayload {

	public enum Action {
		SELECT, RANGE, PLAY, PAUSE, RESUME, STOP
	}

	public static final CustomPacketPayload.Type<MusicNoteActionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_note_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicNoteActionPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, MusicNoteActionPayload::mainHand,
		ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], Action::ordinal), MusicNoteActionPayload::action,
		ByteBufCodecs.STRING_UTF8, MusicNoteActionPayload::track,
		ByteBufCodecs.VAR_INT, MusicNoteActionPayload::range,
		MusicNoteActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
