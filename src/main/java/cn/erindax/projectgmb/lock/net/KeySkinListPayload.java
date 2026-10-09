package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record KeySkinListPayload(boolean mainHand, String current, List<String> skins) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<KeySkinListPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("key_skin_list"));

	public static final StreamCodec<RegistryFriendlyByteBuf, KeySkinListPayload> STREAM_CODEC =
		CustomPacketPayload.codec(KeySkinListPayload::write, KeySkinListPayload::new);

	private KeySkinListPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(mainHand);
		buf.writeUtf(current);
		buf.writeCollection(skins, FriendlyByteBuf::writeUtf);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
