package cn.erindax.projectgmb.dance;

public final class DanceRules {

	public static final int SICK = 0;
	public static final int GOOD = 1;
	public static final int BAD = 2;
	public static final int SHIT = 3;
	public static final int MISS = 4;

	public static final int HIT_WINDOW = 166;
	public static final int MISS_PENALTY = 10;
	public static final int HOLD_BONUS = 150;
	public static final int HOLD_GRACE = 120;
	public static final int MAX_BONUS_COMBO = 50;

	private static final int[] WINDOWS = {45, 90, 135, HIT_WINDOW};
	private static final int[] POINTS = {350, 200, 100, 50};
	private static final float[] WEIGHTS = {1.0F, 0.67F, 0.34F, 0.0F};

	private DanceRules() {
	}

	public static int judge(double diff) {
		double abs = Math.abs(diff);
		for (int i = 0; i < WINDOWS.length; i++) {
			if (abs <= WINDOWS[i]) {
				return i;
			}
		}
		return MISS;
	}

	public static float multiplier(int combo) {
		return 1.0F + Math.min(Math.max(combo, 0), MAX_BONUS_COMBO) * 0.02F;
	}

	public static int points(int judgement, int combo) {
		return Math.round(POINTS[judgement] * multiplier(combo));
	}

	public static float weight(int judgement) {
		return judgement >= 0 && judgement < WEIGHTS.length ? WEIGHTS[judgement] : 0.0F;
	}

	public static long maxScore(DanceChart chart) {
		long total = 0;
		int combo = 0;
		for (DanceChart.Note note : chart.notes()) {
			combo++;
			total += points(SICK, combo);
			if (note.hold() > 0) {
				total += HOLD_BONUS;
			}
		}
		return total;
	}

	public static String grade(DanceStats stats) {
		if (stats.judged() == 0) {
			return "-";
		}
		float accuracy = stats.accuracy();
		if (accuracy >= 0.98F) {
			return "S+";
		}
		if (accuracy >= 0.95F) {
			return "S";
		}
		if (accuracy >= 0.90F) {
			return "A";
		}
		if (accuracy >= 0.80F) {
			return "B";
		}
		if (accuracy >= 0.70F) {
			return "C";
		}
		return "D";
	}
}
