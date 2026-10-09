package cn.erindax.projectgmb.lock;

public enum LockMode {
	EVERY, SINGLE;

	public static LockMode byIndex(int index) {
		LockMode[] values = values();
		return values[Math.max(0, Math.min(values.length - 1, index))];
	}
}
