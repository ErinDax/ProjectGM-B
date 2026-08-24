package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record VanishTogglePayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VanishTogglePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("vanish_toggle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VanishTogglePayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.BOOL, VanishTogglePayload::active, VanishTogglePayload::new);

	@Override
	public CustomPacketPayload.Type<VanishTogglePayload> type() {
		return TYPE;
	}
}
