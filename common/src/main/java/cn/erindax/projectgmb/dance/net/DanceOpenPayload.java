package cn.erindax.projectgmb.dance.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.dance.DanceChart;
import cn.erindax.projectgmb.dance.DanceStanding;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record DanceOpenPayload(int session, DanceChart chart, int audioVersion, List<DanceStanding> players)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DanceOpenPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("dance_open"));

	public static final StreamCodec<FriendlyByteBuf, DanceOpenPayload> STREAM_CODEC = StreamCodec.composite(
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
