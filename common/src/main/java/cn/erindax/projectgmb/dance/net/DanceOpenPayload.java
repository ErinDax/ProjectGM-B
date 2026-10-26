package cn.erindax.projectgmb.dance.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.dance.DanceChart;
import cn.erindax.projectgmb.dance.DanceStanding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record DanceOpenPayload(int session, DanceChart chart, int audioVersion, List<DanceStanding> players)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DanceOpenPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("dance_open"));

	public static final StreamCodec<RegistryFriendlyByteBuf, DanceOpenPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceOpenPayload::session,
		DanceChart.STREAM_CODEC, DanceOpenPayload::chart,
		ByteBufCodecs.INT, DanceOpenPayload::audioVersion,
		DanceStanding.LIST_CODEC, DanceOpenPayload::players,
		DanceOpenPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
