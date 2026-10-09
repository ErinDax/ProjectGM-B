package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.item.MusicNoteItem;
import cn.erindax.projectgmb.music.net.MusicBlockSyncPayload;
import cn.erindax.projectgmb.music.net.MusicBlockUpdatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MusicBlocks {

	private static final Map<GlobalPos, Boolean> POWERED = new HashMap<>();

	private MusicBlocks() {
	}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(MusicBlocks::tick);
	}

	public static void toggle(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack note) {
		MusicBlockState state = MusicBlockState.get(level);
		MusicBlock existing = state.get(pos);
		String track = MusicNoteItem.trackOf(note);
		MusicBlock block = new MusicBlock(track, MusicNoteItem.rangeOf(note));
		if (existing != null && (track.isEmpty() || existing.equals(block))) {
			remove(level, pos);
			player.displayClientMessage(Component.translatable("item.projectgm_b.music_note.detached"), true);
			return;
		}
		if (track.isEmpty()) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.music_note.select_first"), true);
			return;
		}
		if (level.getBlockState(pos).isAir()) {
			return;
		}
		if (existing != null) {
			MusicSessions.stopForBlock(level, pos);
		}
		state.put(pos, block);
		POWERED.put(GlobalPos.of(level.dimension(), pos.immutable()), level.hasNeighborSignal(pos));
		broadcast(level, new MusicBlockUpdatePayload(pos, true, block.track(), block.range()));
		player.displayClientMessage(Component.translatable(
			existing != null ? "item.projectgm_b.music_note.updated" : "item.projectgm_b.music_note.attached",
			MusicNoteItem.displayName(track), block.range()), true);
	}

	public static void remove(ServerLevel level, BlockPos pos) {
		MusicBlockState.get(level).remove(pos);
		POWERED.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		MusicSessions.stopForBlock(level, pos);
		broadcast(level, MusicBlockUpdatePayload.removed(pos));
	}

	public static void sendAll(ServerPlayer player) {
		List<MusicBlockSyncPayload.Entry> entries = new ArrayList<>();
		for (Map.Entry<BlockPos, MusicBlock> entry : MusicBlockState.get(player.serverLevel()).all().entrySet()) {
			entries.add(new MusicBlockSyncPayload.Entry(entry.getKey(), entry.getValue()));
		}
		ServerPlayNetworking.send(player, new MusicBlockSyncPayload(entries));
	}

	private static void tick(ServerLevel level) {
		MusicBlockState state = MusicBlockState.get(level);
		if (state.isEmpty()) {
			return;
		}
		List<BlockPos> gone = null;
		for (Map.Entry<BlockPos, MusicBlock> entry : state.all().entrySet()) {
			BlockPos pos = entry.getKey();
			if (!neighborhoodLoaded(level, pos)) {
				continue;
			}
			if (level.getBlockState(pos).isAir()) {
				if (gone == null) {
					gone = new ArrayList<>();
				}
				gone.add(pos);
				continue;
			}
			boolean powered = level.hasNeighborSignal(pos);
			Boolean previous = POWERED.put(GlobalPos.of(level.dimension(), pos), powered);
			if (previous == null || previous == powered) {
				continue;
			}
			if (powered) {
				MusicSessions.playForBlock(level, pos, entry.getValue().track(), entry.getValue().range());
			} else {
				MusicSessions.stopForBlock(level, pos);
			}
		}
		if (gone != null) {
			for (BlockPos pos : gone) {
				remove(level, pos);
			}
		}
	}

	private static boolean neighborhoodLoaded(ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			if (!level.isLoaded(pos.relative(direction))) {
				return false;
			}
		}
		return true;
	}

	private static void broadcast(ServerLevel level, MusicBlockUpdatePayload payload) {
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
