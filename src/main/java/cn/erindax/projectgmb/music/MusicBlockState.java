package cn.erindax.projectgmb.music;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MusicBlockState extends SavedData {

	private static final String DATA_NAME = "projectgm_b_music_blocks";
	private static final String TAG_BLOCKS = "Blocks";
	private static final String TAG_POS = "Pos";
	private static final String TAG_DATA = "Data";

	private static final SavedData.Factory<MusicBlockState> FACTORY =
		new SavedData.Factory<>(MusicBlockState::new, MusicBlockState::load, null);

	private final Map<BlockPos, MusicBlock> blocks = new HashMap<>();

	public static MusicBlockState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static MusicBlockState load(CompoundTag tag, HolderLookup.Provider registries) {
		MusicBlockState state = new MusicBlockState();
		ListTag list = tag.getList(TAG_BLOCKS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			state.blocks.put(BlockPos.of(entry.getLong(TAG_POS)), MusicBlock.load(entry.getCompound(TAG_DATA)));
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<BlockPos, MusicBlock> entry : blocks.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putLong(TAG_POS, entry.getKey().asLong());
			item.put(TAG_DATA, entry.getValue().save());
			list.add(item);
		}
		tag.put(TAG_BLOCKS, list);
		return tag;
	}

	@Nullable
	public MusicBlock get(BlockPos pos) {
		return blocks.get(pos);
	}

	public void put(BlockPos pos, MusicBlock block) {
		blocks.put(pos.immutable(), block);
		setDirty();
	}

	@Nullable
	public MusicBlock remove(BlockPos pos) {
		MusicBlock removed = blocks.remove(pos);
		if (removed != null) {
			setDirty();
		}
		return removed;
	}

	public boolean isEmpty() {
		return blocks.isEmpty();
	}

	public Map<BlockPos, MusicBlock> all() {
		return Collections.unmodifiableMap(blocks);
	}
}
