package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.client.dance.DanceClient;
import cn.erindax.projectgmb.client.gui.KeySkinScreen;
import cn.erindax.projectgmb.client.gui.LockOwnerScreen;
import cn.erindax.projectgmb.client.gui.LockPasswordScreen;
import cn.erindax.projectgmb.client.gui.MusicNoteScreen;
import cn.erindax.projectgmb.client.gui.PatrollerListScreen;
import cn.erindax.projectgmb.client.gui.PatrollerSettingsScreen;
import cn.erindax.projectgmb.client.lock.ClientLocks;
import cn.erindax.projectgmb.client.lock.LockRenderer;
import cn.erindax.projectgmb.client.music.ClientMusic;
import cn.erindax.projectgmb.client.music.ClientMusicBlocks;
import cn.erindax.projectgmb.client.music.MusicBlockRenderer;
import cn.erindax.projectgmb.client.music.MusicPlayer;
import cn.erindax.projectgmb.client.music.MusicUploader;
import cn.erindax.projectgmb.client.reinforce.ReinforceRenderer;
import cn.erindax.projectgmb.client.render.RemoteTextures;
import cn.erindax.projectgmb.client.seal.SealRenderer;
import cn.erindax.projectgmb.entity.net.PatrollerListPayload;
import cn.erindax.projectgmb.entity.net.PatrollerSettingsPayload;
import cn.erindax.projectgmb.lock.DoorLockClient;
import cn.erindax.projectgmb.lock.LockMode;
import cn.erindax.projectgmb.lock.LockType;
import cn.erindax.projectgmb.lock.net.KeySkinListPayload;
import cn.erindax.projectgmb.lock.net.LockScreenPayload;
import cn.erindax.projectgmb.lock.net.LockSyncPayload;
import cn.erindax.projectgmb.lock.net.LockUpdatePayload;
import cn.erindax.projectgmb.music.net.MusicBlockSyncPayload;
import cn.erindax.projectgmb.music.net.MusicBlockUpdatePayload;
import cn.erindax.projectgmb.music.net.MusicControlPayload;
import cn.erindax.projectgmb.music.net.MusicDataPayload;
import cn.erindax.projectgmb.music.net.MusicMenuPayload;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import cn.erindax.projectgmb.network.VanishSyncPayload;
import cn.erindax.projectgmb.reinforce.Reinforcements;
import cn.erindax.projectgmb.reinforce.net.ReinforceSelectionPayload;
import cn.erindax.projectgmb.reinforce.net.ReinforceSyncPayload;
import cn.erindax.projectgmb.seal.Seals;
import cn.erindax.projectgmb.seal.net.SealSelectionPayload;
import cn.erindax.projectgmb.seal.net.SealSyncPayload;
import cn.erindax.projectgmb.skin.TexturePayload;
import net.minecraft.client.Minecraft;

import java.util.HashSet;
import java.util.List;

public final class ProjectGmBClient {
	private ProjectGmBClient() {
	}

	public static void init() {
		Network.onClient(SyncDoorLocksPayload.TYPE, payload ->
			DoorLockClient.replace(payload.toMap())
		);
		Network.onClient(VanishSyncPayload.TYPE, payload ->
			VanishClient.replace(new HashSet<>(payload.vanished()))
		);
		HostBroadcastClient.register();
		VanishKeyClient.register();
		registerPorted();
	}

	private static void registerPorted() {
		Network.onClient(TexturePayload.TYPE, payload ->
			RemoteTextures.put(payload.kind(), payload.name(), payload.png())
		);
		Network.onClient(LockSyncPayload.TYPE, ClientLocks::replace);
		Network.onClient(LockUpdatePayload.TYPE, payload ->
			ClientLocks.update(payload.pos(), payload.lockType())
		);
		Network.onClient(LockScreenPayload.TYPE, payload ->
			Minecraft.getInstance().setScreen(switch (payload.kind()) {
				case SETUP_PASSWORD -> new LockPasswordScreen(payload.pos(), true);
				case UNLOCK_PASSWORD -> new LockPasswordScreen(payload.pos(), false);
				case OWNER -> new LockOwnerScreen(payload.pos(), LockType.byIndex(payload.lockType()),
					LockMode.byIndex(payload.mode()));
			})
		);
		Network.onClient(KeySkinListPayload.TYPE, payload ->
			Minecraft.getInstance().setScreen(new KeySkinScreen(payload))
		);
		Network.onClient(MusicMenuPayload.TYPE, payload ->
			Minecraft.getInstance().setScreen(new MusicNoteScreen(payload))
		);
		Network.onClient(PatrollerSettingsPayload.TYPE, payload ->
			Minecraft.getInstance().setScreen(new PatrollerSettingsScreen(payload))
		);
		Network.onClient(PatrollerListPayload.TYPE, payload ->
			Minecraft.getInstance().setScreen(new PatrollerListScreen(payload))
		);
		Network.onClient(MusicControlPayload.TYPE, MusicPlayer::handle);
		Network.onClient(MusicDataPayload.TYPE, ClientMusic::receive);
		Network.onClient(MusicBlockSyncPayload.TYPE, ClientMusicBlocks::replace);
		Network.onClient(MusicBlockUpdatePayload.TYPE, payload ->
			ClientMusicBlocks.update(payload.pos(), payload.present(), payload.track(), payload.range())
		);
		Network.onClient(ReinforceSyncPayload.TYPE, payload -> Reinforcements.setClientView(payload.boxes()));
		Network.onClient(ReinforceSelectionPayload.TYPE, ReinforceRenderer::select);
		Network.onClient(SealSyncPayload.TYPE, payload -> Seals.setClientView(payload.boxes()));
		Network.onClient(SealSelectionPayload.TYPE, SealRenderer::select);
		ClientEvents.DISCONNECT.register(client -> {
			RemoteTextures.clear();
			ClientLocks.clear();
			MusicPlayer.clear();
			ClientMusic.clear();
			ClientMusicBlocks.clear();
			MusicUploader.reset();
			Reinforcements.setClientView(List.of());
			ReinforceRenderer.clear();
			Seals.setClientView(List.of());
			SealRenderer.clear();
		});
		LockRenderer.init();
		MusicBlockRenderer.init();
		ReinforceRenderer.init();
		SealRenderer.init();
		MusicUploader.init();
		DanceClient.init();
	}
}
