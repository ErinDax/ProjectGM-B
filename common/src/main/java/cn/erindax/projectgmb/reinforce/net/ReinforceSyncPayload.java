package cn.erindax.projectgmb.reinforce.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

public record ReinforceSyncPayload(List<BoundingBox> boxes) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ReinforceSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("reinforce_sync"));

	public static final StreamCodec<FriendlyByteBuf, ReinforceSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(ReinforceSyncPayload::write, ReinforceSyncPayload::new);

	private ReinforceSyncPayload(FriendlyByteBuf buf) {
		this(buf.readList(ReinforceSyncPayload::readBox));
	}

	private static BoundingBox readBox(FriendlyByteBuf buf) {
		return new BoundingBox(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
	}

	private static void writeBox(FriendlyByteBuf buf, BoundingBox box) {
		buf.writeInt(box.minX());
		buf.writeInt(box.minY());
		buf.writeInt(box.minZ());
		buf.writeInt(box.maxX());
		buf.writeInt(box.maxY());
		buf.writeInt(box.maxZ());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeCollection(boxes, ReinforceSyncPayload::writeBox);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
