package cn.erindax.projectgmb.reinforce.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReinforceSelectionPayload(boolean present, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ReinforceSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("reinforce_selection"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ReinforceSelectionPayload> STREAM_CODEC =
		CustomPacketPayload.codec(ReinforceSelectionPayload::write, ReinforceSelectionPayload::new);

	private ReinforceSelectionPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBlockPos());
	}

	public static ReinforceSelectionPayload cleared() {
		return new ReinforceSelectionPayload(false, BlockPos.ZERO);
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(present);
		buf.writeBlockPos(pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
