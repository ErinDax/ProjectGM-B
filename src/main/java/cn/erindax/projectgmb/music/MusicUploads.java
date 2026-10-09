package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.item.ModComponents;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.item.MusicNoteItem;
import cn.erindax.projectgmb.manage.WandWhitelist;
import cn.erindax.projectgmb.music.net.MusicUploadPayload;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

public final class MusicUploads {

	private static final int MAX_CHUNKS = MusicStore.MAX_BYTES / MusicUploadPayload.CHUNK_BYTES + 1;

	private record Outcome(boolean saved, Component message) {
	}

	private static final class Assembly {
		private final String name;
		private final int version;
		private final byte[][] parts;
		private int received;
		private long size;

		private Assembly(String name, int version, int total) {
			this.name = name;
			this.version = version;
			this.parts = new byte[total][];
		}
	}

	private static final Map<UUID, Assembly> ASSEMBLIES = new HashMap<>();

	private MusicUploads() {
	}

	public static void drop(UUID player) {
		ASSEMBLIES.remove(player);
	}

	public static void receive(ServerPlayer player, MusicUploadPayload payload) {
		InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		if (!WandWhitelist.canManage(player)) {
			if (payload.index() == 0) {
				finish(player, hand, Component.translatable("item.projectgm_b.music_note.upload_denied"));
			}
			return;
		}
		String name = payload.name().strip();
		if (!MusicStore.isValidName(name) || payload.total() <= 0 || payload.total() > MAX_CHUNKS
				|| payload.index() < 0 || payload.index() >= payload.total()) {
			if (payload.index() == 0) {
				finish(player, hand, Component.translatable("item.projectgm_b.music_note.upload_invalid", name));
			}
			return;
		}
		Assembly assembly = ASSEMBLIES.get(player.getUUID());
		if (assembly == null || !assembly.name.equals(name) || assembly.version != payload.version()
				|| assembly.parts.length != payload.total()) {
			assembly = new Assembly(name, payload.version(), payload.total());
			ASSEMBLIES.put(player.getUUID(), assembly);
		}
		if (assembly.parts[payload.index()] == null) {
			assembly.parts[payload.index()] = payload.data();
			assembly.received++;
			assembly.size += payload.data().length;
		}
		if (assembly.size > MusicStore.MAX_BYTES) {
			ASSEMBLIES.remove(player.getUUID());
			finish(player, hand, Component.translatable("item.projectgm_b.music_note.upload_invalid", name));
			return;
		}
		if (assembly.received < assembly.parts.length) {
			return;
		}
		ASSEMBLIES.remove(player.getUUID());
		byte[][] parts = assembly.parts;
		int size = (int) assembly.size;
		int version = assembly.version;
		MinecraftServer server = player.server;
		UUID id = player.getUUID();
		CompletableFuture.supplyAsync(() -> store(name, parts, size, version), Util.ioPool())
			.whenComplete((outcome, error) -> server.execute(() -> complete(server, id, hand, name,
				error == null ? outcome : failed(name, error))));
	}

	private static Outcome store(String name, byte[][] parts, int size, int version) {
		byte[] data = concat(parts, size);
		CRC32 crc = new CRC32();
		crc.update(data);
		if ((int) crc.getValue() != version || !MusicStore.looksValid(name, data)) {
			return new Outcome(false, Component.translatable("item.projectgm_b.music_note.upload_invalid", name));
		}
		try {
			MusicStore.save(name, data);
		} catch (IOException e) {
			return failed(name, e);
		}
		return new Outcome(true, Component.translatable("item.projectgm_b.music_note.upload_ok", MusicNoteItem.displayName(name)));
	}

	private static Outcome failed(String name, Throwable error) {
		ProjectGmB.LOGGER.warn("Failed to save uploaded music {}", name, error);
		return new Outcome(false, Component.translatable("item.projectgm_b.music_note.upload_failed",
			String.valueOf(error.getMessage())));
	}

	private static void complete(MinecraftServer server, UUID id, InteractionHand hand, String name, Outcome outcome) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player == null) {
			return;
		}
		if (outcome.saved()) {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(ModItems.MUSIC_NOTE)) {
				held.set(ModComponents.MUSIC_TRACK, name);
			}
		}
		finish(player, hand, outcome.message());
	}

	private static void finish(ServerPlayer player, InteractionHand hand, Component message) {
		player.displayClientMessage(message, true);
		MusicHandler.openMenu(player, hand);
	}

	private static byte[] concat(byte[][] parts, int size) {
		byte[] data = new byte[size];
		int offset = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, data, offset, part.length);
			offset += part.length;
		}
		return data;
	}
}
