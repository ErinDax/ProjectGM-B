package cn.erindax.projectgmb;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class Mods {
	private Mods() {
	}

	@ExpectPlatform
	public static boolean isLoaded(String id) {
		throw new AssertionError();
	}
}
