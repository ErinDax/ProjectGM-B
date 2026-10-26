package cn.erindax.projectgmb.lock.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record KeySkinListPayload(boolean mainHand, String current, List<String> skins) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<KeySkinListPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("key_skin_list"));

	public static final StreamCodec<FriendlyByteBuf, KeySkinListPayload> STREAM_CODEC =
		CustomPacketPayload.codec(KeySkinListPayload::write, KeySkinListPayload::new);

	private KeySkinListPayload(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeBoolean(mainHand);
		buf.writeUtf(current);
		buf.writeCollection(skins, FriendlyByteBuf::writeUtf);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
