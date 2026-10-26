package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.ByteBufCodecs;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

public record VanishTogglePayload(boolean active) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VanishTogglePayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("vanish_toggle"));
	public static final StreamCodec<FriendlyByteBuf, VanishTogglePayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.BOOL, VanishTogglePayload::active, VanishTogglePayload::new);

	@Override
	public CustomPacketPayload.Type<VanishTogglePayload> type() {
		return TYPE;
	}
}
