package cn.erindax.projectgmb.dance.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.dance.DanceStanding;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record DanceBoardPayload(int session, boolean result, List<DanceStanding> standings)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DanceBoardPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("dance_board"));

	public static final StreamCodec<FriendlyByteBuf, DanceBoardPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DanceBoardPayload::session,
		ByteBufCodecs.BOOL, DanceBoardPayload::result,
		DanceStanding.LIST_CODEC, DanceBoardPayload::standings,
		DanceBoardPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
