package cn.erindax.projectgmb.neoforge.client;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.client.ClientEvents;
import cn.erindax.projectgmb.client.ProjectGmBClient;
import cn.erindax.projectgmb.client.dance.DanceSound;
import cn.erindax.projectgmb.client.render.PatrollerModelLayers;
import cn.erindax.projectgmb.client.render.PatrollerRenderer;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.platform.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ProjectGmB.MOD_ID, dist = Dist.CLIENT)
public final class ProjectGmBNeoForgeClient {
	public ProjectGmBNeoForgeClient(IEventBus modBus) {
		ProjectGmBClient.init();
		DanceSound.setFactory(DanceSoundInstance::new);
		modBus.addListener((RegisterKeyMappingsEvent event) ->
			ClientEvents.keyMappings().forEach(factory -> event.register(factory.get())));
		modBus.addListener((RegisterClientExtensionsEvent event) ->
			event.registerItem(new KeyItemExtensions(), ModItems.KEY));
		modBus.addListener((ModelEvent.ModifyBakingResult event) -> DanceBlockModel.wrap(event.getModels()));
		modBus.addListener((RegisterColorHandlersEvent.Block event) ->
			event.register(DanceBlockModel::tint, ModBlocks.DANCE_BLOCK));
		modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
			PatrollerModelLayers.register(event::registerLayerDefinition));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
			event.registerEntityRenderer(ModEntities.PATROLLER, PatrollerRenderer::new));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> Event.post(ClientEvents.TICK, Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
			Event.post(ClientEvents.DISCONNECT, Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
			ClientEvents.renderHud(event.getGuiGraphics(), event.getPartialTick()));
		NeoForge.EVENT_BUS.addListener(ProjectGmBNeoForgeClient::renderLevel);
	}

	private static void renderLevel(RenderLevelStageEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || minecraft.level == null) {
			return;
		}
		MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
		ClientEvents.renderAfterEntities(event.getPoseStack(), buffers, event.getCamera(), minecraft.level);
		buffers.endLastBatch();
	}
}
