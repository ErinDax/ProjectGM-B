package cn.erindax.projectgmb.entity.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record PatrollerSettingsPayload(int entityId, String name, String skin, boolean slim, int pauseSeconds,
		int detectDiameter, int stareSeconds, boolean nameVisible, boolean showHeldItems, boolean patrolling,
		boolean aggressive, int waypointCount, List<String> skins, boolean fromList, String dimension, int x, int y,
		int z) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PatrollerSettingsPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("patroller_settings"));

	public static final StreamCodec<FriendlyByteBuf, PatrollerSettingsPayload> STREAM_CODEC =
		CustomPacketPayload.codec(PatrollerSettingsPayload::write, PatrollerSettingsPayload::new);

	private PatrollerSettingsPayload(FriendlyByteBuf buf) {
		this(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
			buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
			buf.readVarInt(), buf.readList(FriendlyByteBuf::readUtf), buf.readBoolean(), buf.readUtf(),
			buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
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
		buf.writeVarInt(waypointCount);
		buf.writeCollection(skins, FriendlyByteBuf::writeUtf);
		buf.writeBoolean(fromList);
		buf.writeUtf(dimension);
		buf.writeVarInt(x);
		buf.writeVarInt(y);
		buf.writeVarInt(z);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
