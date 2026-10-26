package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MusicBlockUpdatePayload(BlockPos pos, boolean present, String track, int range)
		implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MusicBlockUpdatePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_block_update"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicBlockUpdatePayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, MusicBlockUpdatePayload::pos,
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
