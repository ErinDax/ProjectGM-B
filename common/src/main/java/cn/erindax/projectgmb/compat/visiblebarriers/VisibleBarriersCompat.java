package cn.erindax.projectgmb.compat.visiblebarriers;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

public final class VisibleBarriersCompat {

	private static final String MAIN_CLASS = "xyz.amymialee.visiblebarriers.VisibleBarriers";
	private static final String TOGGLE_FIELD = "toggleVisible";

	private static volatile BooleanSupplier visible;

	private VisibleBarriersCompat() {
	}

	public static boolean isBarrierVisible() {
		BooleanSupplier supplier = visible;
		if (supplier == null) {
			supplier = resolve();
			visible = supplier;
		}
		return supplier.getAsBoolean();
	}

	private static BooleanSupplier resolve() {
		try {
			Field field = Class.forName(MAIN_CLASS).getField(TOGGLE_FIELD);
			return () -> {
				try {
					return field.getBoolean(null);
				} catch (IllegalAccessException e) {
					return false;
				}
			};
		} catch (ReflectiveOperationException | LinkageError e) {
			return () -> false;
		}
	}
}
