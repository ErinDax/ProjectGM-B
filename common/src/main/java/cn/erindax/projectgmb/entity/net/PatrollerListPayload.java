package cn.erindax.projectgmb.entity.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record PatrollerListPayload(List<Entry> entries) implements CustomPacketPayload {

	public record Entry(String name, String dimension, int x, int y, int z, int waypoints, String skin,
			boolean patrolling, boolean hostile) {

		private static Entry read(FriendlyByteBuf buf) {
			return new Entry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarInt(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeUtf(name);
			buf.writeUtf(dimension);
			buf.writeVarInt(x);
			buf.writeVarInt(y);
			buf.writeVarInt(z);
			buf.writeVarInt(waypoints);
			buf.writeUtf(skin);
			buf.writeBoolean(patrolling);
			buf.writeBoolean(hostile);
		}
	}

	public static final CustomPacketPayload.Type<PatrollerListPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("patroller_list"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PatrollerListPayload> STREAM_CODEC =
		CustomPacketPayload.codec(PatrollerListPayload::write, PatrollerListPayload::new);

	private PatrollerListPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(Entry::read));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
