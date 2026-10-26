package cn.erindax.projectgmb.dance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class DanceState extends SavedData {

	private static final String DATA_NAME = "projectgm_b_dance";
	private static final String ENABLED = "enabled";

	private boolean enabled;

	public static DanceState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(DanceState::load, DanceState::new, DATA_NAME);
	}

	private static DanceState load(CompoundTag tag) {
		DanceState state = new DanceState();
		state.enabled = tag.getBoolean(ENABLED);
		return state;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putBoolean(ENABLED, enabled);
		return tag;
	}

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		setDirty();
	}
}
