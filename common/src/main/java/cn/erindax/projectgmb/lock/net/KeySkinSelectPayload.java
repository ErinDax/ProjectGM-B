package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record KeySkinSelectPayload(boolean mainHand, String skin) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<KeySkinSelectPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("key_skin_select"));

	public static final StreamCodec<FriendlyByteBuf, KeySkinSelectPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, KeySkinSelectPayload::mainHand,
		ByteBufCodecs.STRING_UTF8, KeySkinSelectPayload::skin,
		KeySkinSelectPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
