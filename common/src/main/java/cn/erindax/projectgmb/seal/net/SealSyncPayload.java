package cn.erindax.projectgmb.seal.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

public record SealSyncPayload(List<BoundingBox> boxes) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SealSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("seal_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SealSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(SealSyncPayload::write, SealSyncPayload::new);

	private SealSyncPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(SealSyncPayload::readBox));
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

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(boxes, SealSyncPayload::writeBox);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
