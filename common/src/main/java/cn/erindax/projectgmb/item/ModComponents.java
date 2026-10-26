package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

public final class ModComponents {
	public static final Key<String> KEY_SKIN = new Key<>("key_skin", Tag.TAG_STRING, CompoundTag::getString,
		CompoundTag::putString);
	public static final Key<String> MUSIC_TRACK = new Key<>("music_track", Tag.TAG_STRING, CompoundTag::getString,
		CompoundTag::putString);
	public static final Key<Integer> MUSIC_RANGE = new Key<>("music_range", Tag.TAG_INT, CompoundTag::getInt,
		CompoundTag::putInt);

	private ModComponents() {
	}

	public static final class Key<T> {
		private final String name;
		private final int type;
		private final BiFunction<CompoundTag, String, T> reader;
		private final Writer<T> writer;

		private Key(String path, int type, BiFunction<CompoundTag, String, T> reader, Writer<T> writer) {
			this.name = ProjectGmB.id(path).toString();
			this.type = type;
			this.reader = reader;
			this.writer = writer;
		}

		public T getOrDefault(ItemStack stack, T fallback) {
			CompoundTag tag = stack.getTag();
			return tag != null && tag.contains(name, type) ? reader.apply(tag, name) : fallback;
		}

		public void set(ItemStack stack, T value) {
			writer.write(stack.getOrCreateTag(), name, value);
		}

		public void remove(ItemStack stack) {
			stack.removeTagKey(name);
		}
	}

	@FunctionalInterface
	private interface Writer<T> {
		void write(CompoundTag tag, String name, T value);
	}
}
