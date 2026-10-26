package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record MusicUploadPayload(boolean mainHand, String name, int version, int index, int total, byte[] data)
		implements CustomPacketPayload {

	public static final int CHUNK_BYTES = 30000;

	public static final CustomPacketPayload.Type<MusicUploadPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_upload"));

	public static final StreamCodec<FriendlyByteBuf, MusicUploadPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MusicUploadPayload::write, MusicUploadPayload::new);

	private MusicUploadPayload(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readUtf(), buf.readInt(), buf.readVarInt(), buf.readVarInt(),
			buf.readByteArray(CHUNK_BYTES));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeBoolean(mainHand);
		buf.writeUtf(name);
		buf.writeInt(version);
		buf.writeVarInt(index);
		buf.writeVarInt(total);
		buf.writeByteArray(data);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
