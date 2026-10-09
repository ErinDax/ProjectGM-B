package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MusicControlPayload(UUID session, Action action, String track, int version, int range, int entityId,
		BlockPos pos, int offset) implements CustomPacketPayload {

	public enum Action {
		PLAY, PAUSE, RESUME, STOP
	}

	public static final int NO_ENTITY = -1;

	public static final CustomPacketPayload.Type<MusicControlPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_control"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicControlPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MusicControlPayload::write, MusicControlPayload::new);

	public static MusicControlPayload simple(UUID session, Action action) {
		return new MusicControlPayload(session, action, "", 0, 0, NO_ENTITY, BlockPos.ZERO, 0);
	}

	private MusicControlPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readUUID(), Action.values()[buf.readVarInt()], buf.readUtf(), buf.readInt(), buf.readVarInt(),
			buf.readInt(), buf.readBlockPos(), buf.readVarInt());
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeUUID(session);
		buf.writeVarInt(action.ordinal());
		buf.writeUtf(track);
		buf.writeInt(version);
		buf.writeVarInt(range);
		buf.writeInt(entityId);
		buf.writeBlockPos(pos);
		buf.writeVarInt(offset);
	}

	public boolean followsEntity() {
		return entityId != NO_ENTITY;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
