package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record VanishSyncPayload(List<UUID> vanished) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VanishSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("vanish_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VanishSyncPayload> CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()),
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
