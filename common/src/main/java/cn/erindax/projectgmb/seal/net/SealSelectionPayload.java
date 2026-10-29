package cn.erindax.projectgmb.seal.net;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SealSelectionPayload(boolean present, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SealSelectionPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("seal_selection"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SealSelectionPayload> STREAM_CODEC =
		CustomPacketPayload.codec(SealSelectionPayload::write, SealSelectionPayload::new);

	private SealSelectionPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readBoolean(), buf.readBlockPos());
	}

	public static SealSelectionPayload cleared() {
		return new SealSelectionPayload(false, BlockPos.ZERO);
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
