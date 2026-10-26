package cn.erindax.projectgmb.music.net;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.MusicBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record MusicBlockSyncPayload(List<Entry> entries) implements CustomPacketPayload {

	public record Entry(BlockPos pos, MusicBlock block) {

		static Entry read(FriendlyByteBuf buf) {
			return new Entry(buf.readBlockPos(), new MusicBlock(buf.readUtf(), buf.readVarInt()));
		}

		void write(FriendlyByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeUtf(block.track());
			buf.writeVarInt(block.range());
		}
	}

	public static final CustomPacketPayload.Type<MusicBlockSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ProjectGmB.id("music_block_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MusicBlockSyncPayload> STREAM_CODEC =
		CustomPacketPayload.codec(MusicBlockSyncPayload::write, MusicBlockSyncPayload::new);

	private MusicBlockSyncPayload(RegistryFriendlyByteBuf buf) {
		this(buf.readList(Entry::read));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeCollection(entries, (b, e) -> e.write(b));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
