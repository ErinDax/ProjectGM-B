package cn.erindax.projectgmb.seal.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record SealSelectionPayload(boolean present, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SealSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("seal_selection"));

	public static final StreamCodec<FriendlyByteBuf, SealSelectionPayload> STREAM_CODEC =
		CustomPacketPayload.codec(SealSelectionPayload::write, SealSelectionPayload::new);

	private SealSelectionPayload(FriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBlockPos());
	}

	public static SealSelectionPayload cleared() {
		return new SealSelectionPayload(false, BlockPos.ZERO);
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
