package cn.erindax.projectgmb.fabric;

import net.fabricmc.loader.api.FabricLoader;

public final class ModsImpl {
	private ModsImpl() {
	}

	public static boolean isLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}
}
