package cn.erindax.projectgmb.reinforce.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record ReinforceSelectionPayload(boolean present, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ReinforceSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("reinforce_selection"));

	public static final StreamCodec<FriendlyByteBuf, ReinforceSelectionPayload> STREAM_CODEC =
		CustomPacketPayload.codec(ReinforceSelectionPayload::write, ReinforceSelectionPayload::new);

	private ReinforceSelectionPayload(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBlockPos());
	}

	public static ReinforceSelectionPayload cleared() {
		return new ReinforceSelectionPayload(false, BlockPos.ZERO);
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeBoolean(present);
		buf.writeBlockPos(pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
