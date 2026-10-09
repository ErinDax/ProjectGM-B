package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.music.MusicBlock;
import cn.erindax.projectgmb.music.net.MusicBlockSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ClientMusicBlocks {

	private static final Map<BlockPos, MusicBlock> BLOCKS = new HashMap<>();

	private ClientMusicBlocks() {
	}

	public static Map<BlockPos, MusicBlock> all() {
		return BLOCKS;
	}

	@Nullable
	public static MusicBlock get(BlockPos pos) {
		return BLOCKS.get(pos);
	}

	public static boolean canSee(Player player) {
		return player.isCreative();
	}

	public static void replace(MusicBlockSyncPayload payload) {
		BLOCKS.clear();
		for (MusicBlockSyncPayload.Entry entry : payload.entries()) {
			BLOCKS.put(entry.pos(), entry.block());
		}
	}

	public static void update(BlockPos pos, boolean present, String track, int range) {
		if (present) {
			BLOCKS.put(pos, new MusicBlock(track, range));
		} else {
			BLOCKS.remove(pos);
		}
	}

	public static void clear() {
		BLOCKS.clear();
	}
}
