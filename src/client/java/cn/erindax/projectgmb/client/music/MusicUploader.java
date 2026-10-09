package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.MusicStore;
import cn.erindax.projectgmb.music.net.MusicUploadPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;

public final class MusicUploader {

	public enum Result {
		STARTED, BUSY, UNSUPPORTED, TOO_LARGE, UNREADABLE
	}

	private static final int CHUNKS_PER_TICK = 20;

	@Nullable
	private static byte[] data;
	private static String name = "";
	private static int version;
	private static int total;
	private static int next;
	private static boolean mainHand;
	private static boolean waiting;

	private MusicUploader() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(MusicUploader::tick);
	}

	public static Result start(Path path, boolean hand) {
		if (isBusy()) {
			return Result.BUSY;
		}
		String fileName = path.getFileName().toString();
		if (!MusicStore.isValidName(fileName)) {
			return Result.UNSUPPORTED;
		}
		byte[] bytes;
		try {
			if (Files.size(path) > MusicStore.MAX_BYTES) {
				return Result.TOO_LARGE;
			}
			bytes = Files.readAllBytes(path);
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read {}", path, e);
			return Result.UNREADABLE;
		}
		CRC32 crc = new CRC32();
		crc.update(bytes);
		data = bytes;
		name = fileName;
		version = (int) crc.getValue();
		total = Math.max(1, (bytes.length + MusicUploadPayload.CHUNK_BYTES - 1) / MusicUploadPayload.CHUNK_BYTES);
		next = 0;
		mainHand = hand;
		waiting = false;
		return Result.STARTED;
	}

	public static boolean isBusy() {
		return data != null || waiting;
	}

	public static boolean isWaiting() {
		return waiting;
	}

	public static String name() {
		return name;
	}

	public static int percent() {
		return total == 0 ? 0 : Math.min(100, next * 100 / total);
	}

	public static void reset() {
		data = null;
		waiting = false;
		next = 0;
		total = 0;
	}

	private static void tick(Minecraft minecraft) {
		if (data == null) {
			return;
		}
		if (minecraft.getConnection() == null) {
			reset();
			return;
		}
		for (int i = 0; i < CHUNKS_PER_TICK && next < total; i++) {
			int from = next * MusicUploadPayload.CHUNK_BYTES;
			int to = Math.min(data.length, from + MusicUploadPayload.CHUNK_BYTES);
			ClientPlayNetworking.send(new MusicUploadPayload(mainHand, name, version, next, total,
				Arrays.copyOfRange(data, from, to)));
			next++;
		}
		if (next >= total) {
			data = null;
			waiting = true;
		}
	}
}
