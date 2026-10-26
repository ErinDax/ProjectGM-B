package cn.erindax.projectgmb.reinforce;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReinforceState extends SavedData {

	private static final String DATA_NAME = "projectgm_b_reinforced";
	private static final String TAG_BOXES = "Boxes";

	private final List<BoundingBox> boxes = new ArrayList<>();

	public static ReinforceState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(ReinforceState::load, ReinforceState::new, DATA_NAME);
	}

	private static ReinforceState load(CompoundTag tag) {
		ReinforceState state = new ReinforceState();
		ListTag list = tag.getList(TAG_BOXES, Tag.TAG_INT_ARRAY);
		for (int i = 0; i < list.size(); i++) {
			int[] values = list.getIntArray(i);
			if (values.length == 6) {
				state.boxes.add(new BoundingBox(values[0], values[1], values[2], values[3], values[4], values[5]));
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (BoundingBox box : boxes) {
			list.add(new IntArrayTag(new int[] {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()}));
		}
		tag.put(TAG_BOXES, list);
		return tag;
	}

	public List<BoundingBox> boxes() {
		return Collections.unmodifiableList(boxes);
	}

	public boolean isEmpty() {
		return boxes.isEmpty();
	}

	public boolean contains(BlockPos pos) {
		return Reinforcements.contains(boxes, pos);
	}

	public boolean add(BoundingBox box) {
		for (BoundingBox existing : boxes) {
			if (Reinforcements.same(existing, box)) {
				return false;
			}
		}
		boxes.add(box);
		setDirty();
		return true;
	}

	public int removeContaining(BlockPos pos) {
		int before = boxes.size();
		boxes.removeIf(box -> box.isInside(pos));
		int removed = before - boxes.size();
		if (removed > 0) {
			setDirty();
		}
		return removed;
	}
}
