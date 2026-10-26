package cn.erindax.projectgmb;

import net.minecraft.world.level.GameRules;

public final class ModGameRules {

	public static final GameRules.Key<GameRules.BooleanValue> ADVENTURE_FLOWER_POT = Platform.registerBooleanRule(
		"adventureFlowerPot",
		GameRules.Category.PLAYER,
		false
	);

	private ModGameRules() {
	}

	public static void init() {
	}
}
