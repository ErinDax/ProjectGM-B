package cn.erindax.projectgmb.lock;

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

public class LockState extends SavedData {

	private static final String DATA_NAME = "projectgm_b_locks";
	private static final String TAG_LOCKS = "Locks";
	private static final String TAG_POS = "Pos";
	private static final String TAG_DATA = "Data";

	private static final SavedData.Factory<LockState> FACTORY =
		new SavedData.Factory<>(LockState::new, LockState::load, null);

	private final Map<BlockPos, LockData> locks = new HashMap<>();

	public static LockState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static LockState load(CompoundTag tag, HolderLookup.Provider registries) {
		LockState state = new LockState();
		ListTag list = tag.getList(TAG_LOCKS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			state.locks.put(BlockPos.of(entry.getLong(TAG_POS)), LockData.load(entry.getCompound(TAG_DATA), registries));
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<BlockPos, LockData> entry : locks.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putLong(TAG_POS, entry.getKey().asLong());
			item.put(TAG_DATA, entry.getValue().save(registries));
			list.add(item);
		}
		tag.put(TAG_LOCKS, list);
		return tag;
	}

	@Nullable
	public LockData get(BlockPos pos) {
		return locks.get(pos);
	}

	public void put(BlockPos pos, LockData data) {
		locks.put(pos.immutable(), data);
		setDirty();
	}

	@Nullable
	public LockData remove(BlockPos pos) {
		LockData removed = locks.remove(pos);
		if (removed != null) {
			setDirty();
		}
		return removed;
	}

	public void markDirty() {
		setDirty();
	}

	public Map<BlockPos, LockData> all() {
		return Collections.unmodifiableMap(locks);
	}
}
