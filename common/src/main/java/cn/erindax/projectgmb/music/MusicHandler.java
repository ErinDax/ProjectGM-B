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
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.ModEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class MusicHandler {

	private MusicHandler() {
	}

	public static void init() {
		Network.clientbound(MusicMenuPayload.TYPE, MusicMenuPayload.STREAM_CODEC);
		Network.clientbound(MusicControlPayload.TYPE, MusicControlPayload.STREAM_CODEC);
		Network.clientbound(MusicDataPayload.TYPE, MusicDataPayload.STREAM_CODEC);
		Network.clientbound(MusicBlockSyncPayload.TYPE, MusicBlockSyncPayload.STREAM_CODEC);
		Network.clientbound(MusicBlockUpdatePayload.TYPE, MusicBlockUpdatePayload.STREAM_CODEC);

		Network.serverbound(MusicNoteActionPayload.TYPE, MusicNoteActionPayload.STREAM_CODEC,
			(payload, player) -> onAction(player, payload));
		Network.serverbound(MusicRequestPayload.TYPE, MusicRequestPayload.STREAM_CODEC,
			(payload, player) -> onRequest(player, payload));
		Network.serverbound(MusicAckPayload.TYPE, MusicAckPayload.STREAM_CODEC,
			(payload, player) -> MusicStore.onAck(player, payload));
		Network.serverbound(MusicUploadPayload.TYPE, MusicUploadPayload.STREAM_CODEC,
			(payload, player) -> MusicUploads.receive(player, payload));

		ModEvents.PLAYER_JOIN.register(MusicBlocks::sendAll);
		ModEvents.PLAYER_LEAVE.register(player -> {
			MusicSessions.stopForPlayer(player.server, player.getUUID());
			MusicStore.dropQueue(player.getUUID());
			MusicUploads.drop(player.getUUID());
		});
		ModEvents.PLAYER_CHANGED_DIMENSION.register(player -> {
			MusicSessions.stopForPlayer(player.server, player.getUUID());
			MusicBlocks.sendAll(player);
		});
		ModEvents.PLAYER_RESPAWN.register(MusicBlocks::sendAll);
		ModEvents.SERVER_TICK.register(MusicStore::tick);
		ModEvents.SERVER_TICK.register(MusicSessions::tick);
		ModEvents.SERVER_STOPPED.register(server -> MusicSessions.clear());

		MusicBlocks.init();
		MusicStore.ensureDirectory();
	}

	public static void openMenu(ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!held.is(ModItems.MUSIC_NOTE)) {
			return;
		}
		MusicSessions.Session session = MusicSessions.forPlayer(player.getUUID());
		Network.send(player, new MusicMenuPayload(hand == InteractionHand.MAIN_HAND,
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
