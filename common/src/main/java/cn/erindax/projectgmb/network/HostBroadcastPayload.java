package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record HostBroadcastPayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HostBroadcastPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("host_broadcast"));
	public static final StreamCodec<FriendlyByteBuf, HostBroadcastPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.BOOL, HostBroadcastPayload::active, HostBroadcastPayload::new);

	@Override
	public CustomPacketPayload.Type<HostBroadcastPayload> type() {
		return TYPE;
	}
}
