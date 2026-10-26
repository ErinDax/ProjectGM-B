package cn.erindax.projectgmb.skin;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TexturePayload(String kind, String name, byte[] png) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<TexturePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("texture"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TexturePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, TexturePayload::kind,
		ByteBufCodecs.STRING_UTF8, TexturePayload::name,
		ByteBufCodecs.BYTE_ARRAY, TexturePayload::png,
		TexturePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
