package cn.erindax.projectgmb.dance;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record DanceStanding(UUID id, Component name, DanceStats stats, int status) {

	public static final int PLAYING = 0;
	public static final int FINISHED = 1;
	public static final int QUIT = 2;
	public static final int OFFLINE = 3;

	public static final StreamCodec<RegistryFriendlyByteBuf, DanceStanding> STREAM_CODEC = StreamCodec.composite(
		UUIDUtil.STREAM_CODEC, DanceStanding::id,
		ComponentSerialization.STREAM_CODEC, DanceStanding::name,
		DanceStats.STREAM_CODEC, DanceStanding::stats,
		ByteBufCodecs.VAR_INT, DanceStanding::status,
		DanceStanding::new);

	public static final StreamCodec<RegistryFriendlyByteBuf, List<DanceStanding>> LIST_CODEC =
		STREAM_CODEC.apply(ByteBufCodecs.list(256));

	public static final Comparator<DanceStanding> ORDER = Comparator
		.comparingInt((DanceStanding standing) -> standing.stats().score()).reversed()
		.thenComparing(Comparator.comparingDouble((DanceStanding standing) -> standing.stats().accuracy()).reversed())
		.thenComparing(Comparator.comparingInt((DanceStanding standing) -> standing.stats().maxCombo()).reversed());

	public DanceStanding withStats(DanceStats replacement) {
		return new DanceStanding(id, name, replacement, status);
	}
}
