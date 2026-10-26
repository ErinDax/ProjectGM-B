package cn.erindax.projectgmb.network.codec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
	Type<? extends CustomPacketPayload> type();

	static <B extends FriendlyByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(
			StreamCodec.MemberEncoder<B, T> encoder, StreamCodec.Decoder<B, T> decoder) {
		return StreamCodec.ofMember(encoder, decoder);
	}

	record Type<T extends CustomPacketPayload>(ResourceLocation id) {
	}
}
