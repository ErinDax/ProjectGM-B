package cn.erindax.projectgmb.compat.bcya;

import cn.erindax.projectgmb.Mods;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public final class BcyaMaskCompat {

	private static final String MOD_ID = "bcya";
	private static final String UTIL_CLASS = "cn.erindax.bcya.util.MaskUtil";
	private static final String CHECK_METHOD = "isWearingAnyMask";

	private static volatile Predicate<Player> masked;

	private BcyaMaskCompat() {
	}

	public static boolean isMasked(Player player) {
		Predicate<Player> check = masked;
		if (check == null) {
			check = resolve();
			masked = check;
		}
		return check.test(player);
	}

	private static Predicate<Player> resolve() {
		if (!Mods.isLoaded(MOD_ID)) {
			return player -> false;
		}
		try {
			Method method = Class.forName(UTIL_CLASS).getMethod(CHECK_METHOD, LivingEntity.class);
			return player -> {
				try {
					return (boolean) method.invoke(null, player);
				} catch (ReflectiveOperationException e) {
					return false;
				}
			};
		} catch (ReflectiveOperationException | LinkageError e) {
			return player -> false;
		}
	}
}
