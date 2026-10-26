package cn.erindax.projectgmb.fabric.client;

import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.client.ClientEvents;
import cn.erindax.projectgmb.client.ProjectGmBClient;
import cn.erindax.projectgmb.client.dance.DanceSound;
import cn.erindax.projectgmb.client.render.KeyItemRenderer;
import cn.erindax.projectgmb.client.render.PatrollerModelLayers;
import cn.erindax.projectgmb.client.render.PatrollerRenderer;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.Event;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ProjectGmBFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ProjectGmBClient.init();
		DanceSound.setFactory(DanceSoundInstance::new);
		Network.clientbound().forEach(ProjectGmBFabricClient::registerReceiver);
		ClientEvents.keyMappings().forEach(factory -> KeyBindingHelper.registerKeyBinding(factory.get()));
		ClientTickEvents.END_CLIENT_TICK.register(client -> Event.post(ClientEvents.TICK, client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Event.post(ClientEvents.DISCONNECT, client));
		HudRenderCallback.EVENT.register(ClientEvents::renderHud);
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			if (context.matrixStack() != null && context.consumers() != null && context.world() != null) {
				ClientEvents.renderAfterEntities(context.matrixStack(), context.consumers(), context.camera(),
					context.world());
			}
		});
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BARRIER_PANEL, RenderType.cutout());
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.KEY, new KeyItemRenderer()::render);
		DanceBlockModel.register();
		PatrollerModelLayers.register((layer, definition) ->
			EntityModelLayerRegistry.registerModelLayer(layer, definition::get));
		EntityRendererRegistry.register(ModEntities.PATROLLER, PatrollerRenderer::new);
	}

	private static <T extends CustomPacketPayload> void registerReceiver(Network.Clientbound<T> entry) {
		ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> Network.handleClient(payload));
	}
}
