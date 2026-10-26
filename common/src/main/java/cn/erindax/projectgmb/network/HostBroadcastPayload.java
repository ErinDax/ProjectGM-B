package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HostBroadcastPayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HostBroadcastPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("host_broadcast"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HostBroadcastPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.BOOL, HostBroadcastPayload::active, HostBroadcastPayload::new);

	@Override
	public CustomPacketPayload.Type<HostBroadcastPayload> type() {
		return TYPE;
	}
}
