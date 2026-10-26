package cn.erindax.projectgmb.lock;

public enum LockType {
	PASSWORD, KEY;

	public static LockType byIndex(int index) {
		LockType[] values = values();
		return values[Math.max(0, Math.min(values.length - 1, index))];
	}
}
