package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeySkinSelectPayload(boolean mainHand, String skin) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<KeySkinSelectPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("key_skin_select"));

	public static final StreamCodec<RegistryFriendlyByteBuf, KeySkinSelectPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, KeySkinSelectPayload::mainHand,
		ByteBufCodecs.STRING_UTF8, KeySkinSelectPayload::skin,
		KeySkinSelectPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
