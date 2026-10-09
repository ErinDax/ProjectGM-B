package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.item.ModComponents;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.item.MusicNoteItem;
import cn.erindax.projectgmb.music.net.MusicAckPayload;
import cn.erindax.projectgmb.music.net.MusicBlockSyncPayload;
import cn.erindax.projectgmb.music.net.MusicBlockUpdatePayload;
import cn.erindax.projectgmb.music.net.MusicControlPayload;
import cn.erindax.projectgmb.music.net.MusicDataPayload;
import cn.erindax.projectgmb.music.net.MusicMenuPayload;
import cn.erindax.projectgmb.music.net.MusicNoteActionPayload;
import cn.erindax.projectgmb.music.net.MusicRequestPayload;
import cn.erindax.projectgmb.music.net.MusicUploadPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class MusicHandler {

	private MusicHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(MusicMenuPayload.TYPE, MusicMenuPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MusicControlPayload.TYPE, MusicControlPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MusicDataPayload.TYPE, MusicDataPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MusicBlockSyncPayload.TYPE, MusicBlockSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(MusicBlockUpdatePayload.TYPE, MusicBlockUpdatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MusicNoteActionPayload.TYPE, MusicNoteActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MusicRequestPayload.TYPE, MusicRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MusicUploadPayload.TYPE, MusicUploadPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MusicAckPayload.TYPE, MusicAckPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(MusicNoteActionPayload.TYPE,
			(payload, context) -> onAction(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(MusicRequestPayload.TYPE,
			(payload, context) -> onRequest(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(MusicAckPayload.TYPE,
			(payload, context) -> MusicStore.onAck(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(MusicUploadPayload.TYPE,
			(payload, context) -> MusicUploads.receive(context.player(), payload));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> MusicBlocks.sendAll(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			MusicSessions.stopForPlayer(server, handler.player.getUUID());
			MusicStore.dropQueue(handler.player.getUUID());
			MusicUploads.drop(handler.player.getUUID());
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			MusicSessions.stopForPlayer(player.server, player.getUUID());
			MusicBlocks.sendAll(player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> MusicBlocks.sendAll(newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(MusicStore::tick);
		ServerTickEvents.END_SERVER_TICK.register(MusicSessions::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MusicSessions.clear());

		MusicBlocks.init();
		MusicStore.ensureDirectory();
	}

	public static void openMenu(ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(ModItems.MUSIC_NOTE)) {
			return;
		}
		MusicSessions.Session session = MusicSessions.forPlayer(player.getUUID());
		ServerPlayNetworking.send(player, new MusicMenuPayload(hand == InteractionHand.MAIN_HAND,
			MusicNoteItem.trackOf(held), MusicNoteItem.rangeOf(held),
			session != null, session != null && session.paused(), MusicStore.listAvailable()));
	}

	private static void onAction(ServerPlayer player, MusicNoteActionPayload payload) {
		InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(ModItems.MUSIC_NOTE)) {
			return;
		}
		switch (payload.action()) {
			case SELECT -> select(player, held, payload.track());
			case RANGE -> held.set(ModComponents.MUSIC_RANGE, MusicNoteItem.clampRange(payload.range()));
			case PLAY -> play(player, held);
			case PAUSE -> MusicSessions.pausePlayer(player);
			case RESUME -> MusicSessions.resumePlayer(player);
			case STOP -> MusicSessions.stopForPlayer(player.server, player.getUUID());
		}
	}

	private static void select(ServerPlayer player, ItemStack held, String track) {
		if (track.isEmpty()) {
			held.remove(ModComponents.MUSIC_TRACK);
			return;
		}
		if (!MusicStore.exists(track)) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.music_note.missing", track), true);
			return;
		}
		held.set(ModComponents.MUSIC_TRACK, track);
	}

	private static void play(ServerPlayer player, ItemStack held) {
		String track = MusicNoteItem.trackOf(held);
		if (track.isEmpty()) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.music_note.select_first"), true);
			return;
		}
		if (!MusicSessions.playForPlayer(player, track, MusicNoteItem.rangeOf(held))) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.music_note.missing", track), true);
		}
	}

	private static void onRequest(ServerPlayer player, MusicRequestPayload payload) {
		if (MusicStore.isValidName(payload.track())) {
			MusicStore.sendTo(player, payload.track());
		}
	}
}
