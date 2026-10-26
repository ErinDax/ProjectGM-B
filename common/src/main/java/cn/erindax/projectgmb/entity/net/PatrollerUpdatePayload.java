package cn.erindax.projectgmb.entity.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record PatrollerUpdatePayload(String originalName, String name, String skin, boolean slim, int pauseSeconds,
		int detectDiameter, int stareSeconds, boolean nameVisible, boolean showHeldItems, boolean patrolling,
		boolean aggressive, boolean fromList) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PatrollerUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("patroller_update"));

	public static final StreamCodec<FriendlyByteBuf, PatrollerUpdatePayload> STREAM_CODEC =
		CustomPacketPayload.codec(PatrollerUpdatePayload::write, PatrollerUpdatePayload::new);

	private PatrollerUpdatePayload(FriendlyByteBuf buf) {
		this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
			buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
			buf.readBoolean());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeUtf(originalName);
		buf.writeUtf(name);
		buf.writeUtf(skin);
		buf.writeBoolean(slim);
		buf.writeVarInt(pauseSeconds);
		buf.writeVarInt(detectDiameter);
		buf.writeVarInt(stareSeconds);
		buf.writeBoolean(nameVisible);
		buf.writeBoolean(showHeldItems);
		buf.writeBoolean(patrolling);
		buf.writeBoolean(aggressive);
		buf.writeBoolean(fromList);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
