package cn.erindax.projectgmb.neoforge;

import net.neoforged.fml.loading.LoadingModList;

public final class ModsImpl {
	private ModsImpl() {
	}

	public static boolean isLoaded(String id) {
		return LoadingModList.get().getModFileById(id) != null;
	}
}
