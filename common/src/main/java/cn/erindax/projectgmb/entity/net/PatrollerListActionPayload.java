package cn.erindax.projectgmb.entity.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PatrollerListActionPayload(Action action, String name) implements CustomPacketPayload {

	public enum Action {
		REFRESH, EDIT, TELEPORT, REMOVE, RECORD
	}

	public static final CustomPacketPayload.Type<PatrollerListActionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("patroller_list_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PatrollerListActionPayload> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.VAR_INT.map(i -> Action.values()[i], Action::ordinal), PatrollerListActionPayload::action,
			ByteBufCodecs.STRING_UTF8, PatrollerListActionPayload::name,
			PatrollerListActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
