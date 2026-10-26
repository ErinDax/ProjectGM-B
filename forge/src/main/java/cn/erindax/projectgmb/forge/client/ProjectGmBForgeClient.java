package cn.erindax.projectgmb.forge.client;

import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.client.ClientEvents;
import cn.erindax.projectgmb.client.ProjectGmBClient;
import cn.erindax.projectgmb.client.dance.DanceSound;
import cn.erindax.projectgmb.client.render.PatrollerModelLayers;
import cn.erindax.projectgmb.client.render.PatrollerRenderer;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.platform.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ProjectGmBForgeClient {
	private ProjectGmBForgeClient() {
	}

	public static void init(IEventBus modBus) {
		ProjectGmBClient.init();
		DanceSound.setFactory(DanceSoundInstance::new);
		modBus.addListener((RegisterKeyMappingsEvent event) ->
			ClientEvents.keyMappings().forEach(factory -> event.register(factory.get())));
		modBus.addListener((ModelEvent.ModifyBakingResult event) -> DanceBlockModel.wrap(event.getModels()));
		modBus.addListener((RegisterColorHandlersEvent.Block event) ->
			event.register(DanceBlockModel::tint, ModBlocks.DANCE_BLOCK));
		modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
			PatrollerModelLayers.register(event::registerLayerDefinition));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
			event.registerEntityRenderer(ModEntities.PATROLLER, PatrollerRenderer::new));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				Event.post(ClientEvents.TICK, Minecraft.getInstance());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
			Event.post(ClientEvents.DISCONNECT, Minecraft.getInstance()));
		MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
			ClientEvents.renderHud(event.getGuiGraphics(), event.getPartialTick()));
		MinecraftForge.EVENT_BUS.addListener(ProjectGmBForgeClient::renderLevel);
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
