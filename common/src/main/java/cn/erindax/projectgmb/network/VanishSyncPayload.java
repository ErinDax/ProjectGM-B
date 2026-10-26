package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record VanishSyncPayload(List<UUID> vanished) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VanishSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("vanish_sync"));
	public static final StreamCodec<FriendlyByteBuf, VanishSyncPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.UUID.apply(ByteBufCodecs.list()),
		VanishSyncPayload::vanished,
		VanishSyncPayload::new
	);

	@Override
	public CustomPacketPayload.Type<VanishSyncPayload> type() {
		return TYPE;
	}

	public static VanishSyncPayload of(Collection<UUID> vanished) {
		return new VanishSyncPayload(new ArrayList<>(vanished));
	}
}
