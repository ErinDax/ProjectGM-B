package cn.erindax.projectgmb.lock;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class LockData {

	private static final String TAG_TYPE = "Type";
	private static final String TAG_OWNER = "Owner";
	private static final String TAG_OWNER_NAME = "OwnerName";
	private static final String TAG_MODE = "Mode";
	private static final String TAG_HASH = "Hash";
	private static final String TAG_KEY = "Key";
	private static final String TAG_UNLOCKED = "Unlocked";

	private final LockType type;
	private final UUID owner;
	private final String ownerName;
	private LockMode mode = LockMode.EVERY;
	private String passwordHash = "";
	private ItemStack key = ItemStack.EMPTY;
	private final Set<UUID> unlocked = new HashSet<>();

	public LockData(LockType type, UUID owner, String ownerName) {
		this.type = type;
		this.owner = owner;
		this.ownerName = ownerName;
	}

	public LockType type() {
		return type;
	}

	public UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	public LockMode mode() {
		return mode;
	}

	public void setMode(LockMode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			unlocked.clear();
		}
	}

	public void setPasswordHash(String hash) {
		passwordHash = hash;
	}

	public boolean matchesPassword(String password) {
		return !passwordHash.isEmpty() && passwordHash.equals(Locks.hash(password));
	}

	public ItemStack key() {
		return key;
	}

	public void setKey(ItemStack stack) {
		key = stack.copyWithCount(1);
	}

	public boolean matchesKey(ItemStack stack) {
		return !key.isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(key, stack);
	}

	public boolean isOwner(UUID id) {
		return owner.equals(id);
	}

	public boolean isUnlockedFor(UUID id) {
		return mode == LockMode.SINGLE && unlocked.contains(id);
	}

	public void markUnlocked(UUID id) {
		if (mode == LockMode.SINGLE) {
			unlocked.add(id);
		}
	}

	public CompoundTag save(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putByte(TAG_TYPE, (byte) type.ordinal());
		tag.putUUID(TAG_OWNER, owner);
		tag.putString(TAG_OWNER_NAME, ownerName);
		tag.putByte(TAG_MODE, (byte) mode.ordinal());
		tag.putString(TAG_HASH, passwordHash);
		if (!key.isEmpty()) {
			tag.put(TAG_KEY, key.save(registries));
		}
		ListTag list = new ListTag();
		for (UUID id : unlocked) {
			list.add(NbtUtils.createUUID(id));
		}
		tag.put(TAG_UNLOCKED, list);
		return tag;
	}

	public static LockData load(CompoundTag tag, HolderLookup.Provider registries) {
		LockData data = new LockData(LockType.byIndex(tag.getByte(TAG_TYPE)), tag.getUUID(TAG_OWNER),
			tag.getString(TAG_OWNER_NAME));
		data.mode = LockMode.byIndex(tag.getByte(TAG_MODE));
		data.passwordHash = tag.getString(TAG_HASH);
		if (tag.contains(TAG_KEY)) {
			data.key = ItemStack.parse(registries, tag.get(TAG_KEY)).orElse(ItemStack.EMPTY);
		}
		ListTag list = tag.getList(TAG_UNLOCKED, Tag.TAG_INT_ARRAY);
		for (Tag entry : list) {
			data.unlocked.add(NbtUtils.loadUUID(entry));
		}
		return data;
	}
}
