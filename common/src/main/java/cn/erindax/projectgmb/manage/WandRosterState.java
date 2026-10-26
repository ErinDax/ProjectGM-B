package cn.erindax.projectgmb.manage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandRosterState extends SavedData {

	public static final int MAX_GROUP = 16;

	public record Member(String name, String group, boolean hidden) {

		private boolean isEmpty() {
			return group.isEmpty() && !hidden;
		}
	}

	private static final String DATA_NAME = "projectgm_b_wand_roster";
	private static final String TAG_MEMBERS = "Members";
	private static final String TAG_ID = "Id";
	private static final String TAG_NAME = "Name";
	private static final String TAG_GROUP = "Group";
	private static final String TAG_HIDDEN = "Hidden";

	private static final SavedData.Factory<WandRosterState> FACTORY =
		new SavedData.Factory<>(WandRosterState::new, WandRosterState::load, null);

	private final Map<UUID, Member> members = new HashMap<>();

	public static WandRosterState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	private static WandRosterState load(CompoundTag tag, HolderLookup.Provider registries) {
		WandRosterState state = new WandRosterState();
		ListTag list = tag.getList(TAG_MEMBERS, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			if (!entry.hasUUID(TAG_ID)) {
				continue;
			}
			Member member = new Member(entry.getString(TAG_NAME), entry.getString(TAG_GROUP),
				entry.getBoolean(TAG_HIDDEN));
			if (!member.isEmpty()) {
				state.members.put(entry.getUUID(TAG_ID), member);
			}
		}
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, Member> entry : members.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putUUID(TAG_ID, entry.getKey());
			item.putString(TAG_NAME, entry.getValue().name());
			item.putString(TAG_GROUP, entry.getValue().group());
			item.putBoolean(TAG_HIDDEN, entry.getValue().hidden());
			list.add(item);
		}
		tag.put(TAG_MEMBERS, list);
		return tag;
	}

	public boolean isHidden(UUID player) {
		Member member = members.get(player);
		return member != null && member.hidden();
	}

	public String groupOf(UUID player) {
		Member member = members.get(player);
		return member == null ? "" : member.group();
	}

	public boolean setHidden(UUID player, String name, boolean hidden) {
		Member current = members.getOrDefault(player, new Member(name, "", false));
		put(player, new Member(name, current.group(), hidden));
		return current.hidden() != hidden;
	}

	public boolean setGroup(UUID player, String name, String group) {
		Member current = members.getOrDefault(player, new Member(name, "", false));
		put(player, new Member(name, group, current.hidden()));
		return !current.group().equals(group);
	}

	public Map<UUID, Member> all() {
		return Collections.unmodifiableMap(members);
	}

	private void put(UUID player, Member member) {
		if (member.isEmpty()) {
			members.remove(player);
		} else {
			members.put(player, member);
		}
		setDirty();
	}
}
