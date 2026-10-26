package cn.erindax.projectgmb.dance;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

public record DanceStats(int score, int combo, int maxCombo, int sick, int good, int bad, int shit, int miss) {

	public static final DanceStats EMPTY = new DanceStats(0, 0, 0, 0, 0, 0, 0, 0);

	public static final StreamCodec<ByteBuf, DanceStats> STREAM_CODEC = StreamCodec.of(DanceStats::write,
		DanceStats::read);

	public int hits() {
		return sick + good + bad + shit;
	}

	public int judged() {
		return hits() + miss;
	}

	public float accuracy() {
		int judged = judged();
		if (judged == 0) {
			return 0.0F;
		}
		float total = sick * DanceRules.weight(DanceRules.SICK) + good * DanceRules.weight(DanceRules.GOOD)
			+ bad * DanceRules.weight(DanceRules.BAD) + shit * DanceRules.weight(DanceRules.SHIT);
		return total / judged;
	}

	public String accuracyText() {
		return judged() == 0 ? "--" : String.format(Locale.ROOT, "%.2f%%", accuracy() * 100.0F);
	}

	public boolean fullCombo() {
		return miss == 0 && judged() > 0;
	}

	public String grade() {
		return DanceRules.grade(this);
	}

	private static void write(ByteBuf buf, DanceStats stats) {
		ByteBufCodecs.VAR_INT.encode(buf, stats.score);
		ByteBufCodecs.VAR_INT.encode(buf, stats.combo);
		ByteBufCodecs.VAR_INT.encode(buf, stats.maxCombo);
		ByteBufCodecs.VAR_INT.encode(buf, stats.sick);
		ByteBufCodecs.VAR_INT.encode(buf, stats.good);
		ByteBufCodecs.VAR_INT.encode(buf, stats.bad);
		ByteBufCodecs.VAR_INT.encode(buf, stats.shit);
		ByteBufCodecs.VAR_INT.encode(buf, stats.miss);
	}

	private static DanceStats read(ByteBuf buf) {
		return new DanceStats(
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf));
	}
}
