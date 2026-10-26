package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record MusicBlockUpdatePayload(BlockPos pos, boolean present, String track, int range)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicBlockUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_block_update"));

	public static final StreamCodec<FriendlyByteBuf, MusicBlockUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BLOCK_POS, MusicBlockUpdatePayload::pos,
		ByteBufCodecs.BOOL, MusicBlockUpdatePayload::present,
		ByteBufCodecs.STRING_UTF8, MusicBlockUpdatePayload::track,
		ByteBufCodecs.VAR_INT, MusicBlockUpdatePayload::range,
		MusicBlockUpdatePayload::new);

	public static MusicBlockUpdatePayload removed(BlockPos pos) {
		return new MusicBlockUpdatePayload(pos, false, "", 0);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
