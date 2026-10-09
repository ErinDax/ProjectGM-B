package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.client.mixin.SoundEngineAccessor;
import cn.erindax.projectgmb.client.mixin.SoundManagerAccessor;
import cn.erindax.projectgmb.music.net.MusicControlPayload;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MusicPlayer {

	private static final String SOUND_PREFIX = "sounds/" + MusicSoundInstance.PATH_PREFIX;
	private static final String SOUND_SUFFIX = ".ogg";
	private static final long NANOS_PER_MILLI = 1_000_000L;

	private static final class Pending {
		private final MusicControlPayload payload;
		private final long receivedAt;
		private long pausedAt;
		private long pausedTotal;

		private Pending(MusicControlPayload payload, long receivedAt) {
			this.payload = payload;
			this.receivedAt = receivedAt;
		}

		private boolean paused() {
			return pausedAt != 0L;
		}

		private long offsetMillis(long now) {
			long end = paused() ? pausedAt : now;
			return payload.offset() + Math.max(0L, end - receivedAt - pausedTotal) / NANOS_PER_MILLI;
		}
	}

	private static final Map<UUID, MusicSoundInstance> PLAYING = new ConcurrentHashMap<>();
	private static final Map<UUID, Pending> WAITING = new HashMap<>();

	private MusicPlayer() {
	}

	public static void handle(MusicControlPayload payload) {
		switch (payload.action()) {
			case PLAY -> play(payload);
			case PAUSE -> pause(payload.session());
			case RESUME -> resume(payload.session());
			case STOP -> stop(payload.session());
		}
	}

	private static void play(MusicControlPayload payload) {
		stop(payload.session());
		Pending pending = new Pending(payload, System.nanoTime());
		if (ClientMusic.has(payload.track(), payload.version())) {
			start(pending);
		} else {
			WAITING.put(payload.session(), pending);
			ClientMusic.request(payload.track(), payload.version());
		}
	}

	private static void pause(UUID session) {
		Pending pending = WAITING.get(session);
		if (pending == null) {
			withChannel(session, Channel::pause);
		} else if (!pending.paused()) {
			pending.pausedAt = System.nanoTime();
		}
	}

	private static void resume(UUID session) {
		Pending pending = WAITING.get(session);
		if (pending == null) {
			withChannel(session, Channel::unpause);
			return;
		}
		if (pending.paused()) {
			pending.pausedTotal += System.nanoTime() - pending.pausedAt;
			pending.pausedAt = 0L;
		}
		if (ClientMusic.get(pending.payload.track()) != null) {
			WAITING.remove(session);
			start(pending);
		}
	}

	static void onTrackReady(String track) {
		List<Pending> ready = new ArrayList<>();
		Iterator<Pending> it = WAITING.values().iterator();
		while (it.hasNext()) {
			Pending pending = it.next();
			if (pending.payload.track().equals(track) && !pending.paused()) {
				ready.add(pending);
				it.remove();
			}
		}
		ready.forEach(MusicPlayer::start);
	}

	private static void start(Pending pending) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		MusicControlPayload payload = pending.payload;
		long now = System.nanoTime();
		MusicSoundInstance instance = new MusicSoundInstance(payload.session(), payload.track(), payload.range(),
			payload.entityId(), payload.pos(), pending.offsetMillis(now), now);
		PLAYING.put(payload.session(), instance);
		minecraft.getSoundManager().play(instance);
	}

	public static void stop(UUID session) {
		WAITING.remove(session);
		MusicSoundInstance instance = PLAYING.remove(session);
		if (instance != null) {
			Minecraft.getInstance().getSoundManager().stop(instance);
		}
	}

	public static void clear() {
		WAITING.clear();
		SoundManager manager = Minecraft.getInstance().getSoundManager();
		for (MusicSoundInstance instance : PLAYING.values()) {
			manager.stop(instance);
		}
		PLAYING.clear();
	}

	static boolean isTrackInUse(String track) {
		for (MusicSoundInstance instance : PLAYING.values()) {
			if (instance.track().equals(track)) {
				return true;
			}
		}
		for (Pending pending : WAITING.values()) {
			if (pending.payload.track().equals(track)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public static CompletableFuture<AudioStream> openStream(ResourceLocation location) {
		if (!location.getNamespace().equals(ProjectGmB.MOD_ID)) {
			return null;
		}
		String path = location.getPath();
		if (!path.startsWith(SOUND_PREFIX) || !path.endsWith(SOUND_SUFFIX)) {
			return null;
		}
		UUID session;
		try {
			session = UUID.fromString(path.substring(SOUND_PREFIX.length(), path.length() - SOUND_SUFFIX.length()));
		} catch (IllegalArgumentException e) {
			return null;
		}
		MusicSoundInstance instance = PLAYING.get(session);
		byte[] data = instance == null ? null : ClientMusic.get(instance.track());
		if (data == null) {
			return CompletableFuture.failedFuture(new IOException("Music data for session " + session + " is unavailable"));
		}
		String track = instance.track();
		long offsetMillis = instance.offsetMillis();
		long offsetAt = instance.offsetAt();
		return CompletableFuture.supplyAsync(() -> {
			try {
				return SeekingAudioStream.seek(open(track, data), offsetMillis, offsetAt);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, Util.backgroundExecutor());
	}

	private static AudioStream open(String track, byte[] data) throws IOException {
		if (track.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
			return new Mp3AudioStream(new ByteArrayInputStream(data));
		}
		return new JOrbisAudioStream(new ByteArrayInputStream(data));
	}

	private static void withChannel(UUID session, Consumer<Channel> action) {
		MusicSoundInstance instance = PLAYING.get(session);
		if (instance == null) {
			return;
		}
		SoundEngine engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).projectgmb$getSoundEngine();
		ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).projectgmb$getInstanceToChannel().get(instance);
		if (handle != null) {
			handle.execute(action);
		}
	}
}
