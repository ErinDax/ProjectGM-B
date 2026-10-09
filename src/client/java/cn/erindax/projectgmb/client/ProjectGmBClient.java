package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.client.dance.DanceClient;
import cn.erindax.projectgmb.client.gui.KeySkinScreen;
import cn.erindax.projectgmb.client.gui.LockOwnerScreen;
import cn.erindax.projectgmb.client.gui.LockPasswordScreen;
import cn.erindax.projectgmb.client.gui.MusicNoteScreen;
import cn.erindax.projectgmb.client.lock.ClientLocks;
import cn.erindax.projectgmb.client.lock.LockRenderer;
import cn.erindax.projectgmb.client.music.ClientMusic;
import cn.erindax.projectgmb.client.music.ClientMusicBlocks;
import cn.erindax.projectgmb.client.music.MusicBlockRenderer;
import cn.erindax.projectgmb.client.music.MusicPlayer;
import cn.erindax.projectgmb.client.music.MusicUploader;
import cn.erindax.projectgmb.client.render.KeyItemRenderer;
import cn.erindax.projectgmb.client.render.RemoteTextures;
import cn.erindax.projectgmb.item.ModItems;
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
import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import cn.erindax.projectgmb.network.VanishSyncPayload;
import cn.erindax.projectgmb.skin.TexturePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.RenderType;

import java.util.HashSet;

public class ProjectGmBClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncDoorLocksPayload.TYPE, (payload, context) ->
			DoorLockClient.replace(payload.toMap())
		);
		ClientPlayNetworking.registerGlobalReceiver(VanishSyncPayload.TYPE, (payload, context) ->
			VanishClient.replace(new HashSet<>(payload.vanished()))
		);
		HostBroadcastClient.register();
		VanishKeyClient.register();
		registerPorted();
	}

	private static void registerPorted() {
		ClientPlayNetworking.registerGlobalReceiver(TexturePayload.TYPE, (payload, context) ->
			RemoteTextures.put(payload.kind(), payload.name(), payload.png())
		);
		ClientPlayNetworking.registerGlobalReceiver(LockSyncPayload.TYPE, (payload, context) ->
			ClientLocks.replace(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(LockUpdatePayload.TYPE, (payload, context) ->
			ClientLocks.update(payload.pos(), payload.lockType())
		);
		ClientPlayNetworking.registerGlobalReceiver(LockScreenPayload.TYPE, (payload, context) ->
			context.client().setScreen(switch (payload.kind()) {
				case SETUP_PASSWORD -> new LockPasswordScreen(payload.pos(), true);
				case UNLOCK_PASSWORD -> new LockPasswordScreen(payload.pos(), false);
				case OWNER -> new LockOwnerScreen(payload.pos(), LockType.byIndex(payload.lockType()),
					LockMode.byIndex(payload.mode()));
			})
		);
		ClientPlayNetworking.registerGlobalReceiver(KeySkinListPayload.TYPE, (payload, context) ->
			context.client().setScreen(new KeySkinScreen(payload))
		);
		ClientPlayNetworking.registerGlobalReceiver(MusicMenuPayload.TYPE, (payload, context) ->
			context.client().setScreen(new MusicNoteScreen(payload))
		);
		ClientPlayNetworking.registerGlobalReceiver(MusicControlPayload.TYPE, (payload, context) ->
			MusicPlayer.handle(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(MusicDataPayload.TYPE, (payload, context) ->
			ClientMusic.receive(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(MusicBlockSyncPayload.TYPE, (payload, context) ->
			ClientMusicBlocks.replace(payload)
		);
		ClientPlayNetworking.registerGlobalReceiver(MusicBlockUpdatePayload.TYPE, (payload, context) ->
			ClientMusicBlocks.update(payload.pos(), payload.present(), payload.track(), payload.range())
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			RemoteTextures.clear();
			ClientLocks.clear();
			MusicPlayer.clear();
			ClientMusic.clear();
			ClientMusicBlocks.clear();
			MusicUploader.reset();
		});
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BARRIER_PANEL, RenderType.cutout());
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.KEY, new KeyItemRenderer());
		LockRenderer.init();
		MusicBlockRenderer.init();
		MusicUploader.init();
		DanceClient.init();
	}
}
