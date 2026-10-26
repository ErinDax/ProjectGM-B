package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.item.MusicNoteItem;
import net.minecraft.nbt.CompoundTag;

public record MusicBlock(String track, int range) {

	private static final String TAG_TRACK = "Track";
	private static final String TAG_RANGE = "Range";

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString(TAG_TRACK, track);
		tag.putInt(TAG_RANGE, range);
		return tag;
	}

	public static MusicBlock load(CompoundTag tag) {
		return new MusicBlock(tag.getString(TAG_TRACK), MusicNoteItem.clampRange(tag.getInt(TAG_RANGE)));
	}
}
