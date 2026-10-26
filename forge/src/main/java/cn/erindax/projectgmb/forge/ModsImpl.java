package cn.erindax.projectgmb.forge;

import net.minecraftforge.fml.loading.LoadingModList;

public final class ModsImpl {
	private ModsImpl() {
	}

	public static boolean isLoaded(String id) {
		return LoadingModList.get().getModFileById(id) != null;
	}
}
