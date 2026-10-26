package cn.erindax.projectgmb.neoforge;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.entity.PatrollerEntity;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.Event;
import cn.erindax.projectgmb.platform.ModEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Mod(ProjectGmB.MOD_ID)
public final class ProjectGmBNeoForge {
	private static final String PROTOCOL = "1";

	public ProjectGmBNeoForge(IEventBus modBus) {
		ProjectGmB.init();
		modBus.addListener(ProjectGmBNeoForge::registerContent);
		modBus.addListener(ProjectGmBNeoForge::registerPayloads);
		modBus.addListener(ProjectGmBNeoForge::buildCreativeTabs);
		modBus.addListener((EntityAttributeCreationEvent event) ->
			event.put(ModEntities.PATROLLER, PatrollerEntity.createAttributes().build()));
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
			Event.post(ModEvents.REGISTER_COMMANDS, event.getDispatcher()));
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> Event.post(ModEvents.SERVER_STARTED, event.getServer()));
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Event.post(ModEvents.SERVER_STOPPED, event.getServer()));
		NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null) {
				Event.post(ModEvents.DATA_PACK_RELOADED, event.getPlayerList().getServer());
			}
		});
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> Event.post(ModEvents.SERVER_TICK, event.getServer()));
		NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Event.post(ModEvents.LEVEL_TICK, level);
			}
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> postPlayer(ModEvents.PLAYER_JOIN, event));
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> postPlayer(ModEvents.PLAYER_LEAVE, event));
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) ->
			postPlayer(ModEvents.PLAYER_CHANGED_DIMENSION, event));
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> postPlayer(ModEvents.PLAYER_RESPAWN, event));
		NeoForge.EVENT_BUS.addListener(ProjectGmBNeoForge::onUseBlock);
		NeoForge.EVENT_BUS.addListener(ProjectGmBNeoForge::onAttackEntity);
		NeoForge.EVENT_BUS.addListener(ProjectGmBNeoForge::onBeforeBreak);
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ProjectGmBNeoForge::onAfterBreak);
	}

	private static void registerContent(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.BLOCK)) {
			ProjectGmB.registerContent();
		}
	}

	private static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS)) {
			event.accept(ModItems.BARRIER_PANEL);
		}
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(PROTOCOL).optional().executesOn(HandlerThread.MAIN);
		Map<CustomPacketPayload.Type<?>, Network.Serverbound<?>> serverbound = new LinkedHashMap<>();
		for (Network.Serverbound<?> entry : Network.serverbound()) {
			serverbound.put(entry.type(), entry);
		}
		for (Network.Clientbound<?> entry : Network.clientbound()) {
			Network.Serverbound<?> both = serverbound.remove(entry.type());
			if (both != null) {
				registerBidirectional(registrar, both);
			} else {
				registerClientbound(registrar, entry);
			}
		}
		for (Network.Serverbound<?> entry : serverbound.values()) {
			registerServerbound(registrar, entry);
		}
	}

	private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar,
			Network.Clientbound<T> entry) {
		registrar.playToClient(entry.type(), entry.codec(), (payload, context) -> Network.handleClient(payload));
	}

	private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar,
			Network.Serverbound<T> entry) {
		registrar.playToServer(entry.type(), entry.codec(), (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				entry.handler().accept(payload, player);
			}
		});
	}

	private static <T extends CustomPacketPayload> void registerBidirectional(PayloadRegistrar registrar,
			Network.Serverbound<T> entry) {
		registrar.playBidirectional(entry.type(), entry.codec(), (payload, context) -> {
			if (context.flow() == PacketFlow.CLIENTBOUND) {
				Network.handleClient(payload);
			} else if (context.player() instanceof ServerPlayer player) {
				entry.handler().accept(payload, player);
			}
		});
	}

	private static void postPlayer(Event<Consumer<ServerPlayer>> event, PlayerEvent playerEvent) {
		if (playerEvent.getEntity() instanceof ServerPlayer player) {
			Event.post(event, player);
		}
	}

	private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
		InteractionResult result = ModEvents.useBlock(event.getEntity(), event.getLevel(), event.getHand(),
			event.getHitVec());
		if (result != InteractionResult.PASS) {
			event.setCanceled(true);
			event.setCancellationResult(result);
		}
	}

	private static void onAttackEntity(AttackEntityEvent event) {
		Player player = event.getEntity();
		if (ModEvents.attackEntity(player, player.level(), InteractionHand.MAIN_HAND, event.getTarget())
				!= InteractionResult.PASS) {
			event.setCanceled(true);
		}
	}

	private static void onBeforeBreak(BlockEvent.BreakEvent event) {
		if (event.getLevel() instanceof Level level && !ModEvents.beforeBreak(level, event.getPlayer(), event.getPos(),
				event.getState(), level.getBlockEntity(event.getPos()))) {
			event.setCanceled(true);
		}
	}

	private static void onAfterBreak(BlockEvent.BreakEvent event) {
		if (event.getLevel() instanceof Level level) {
			ModEvents.afterBreak(level, event.getPlayer(), event.getPos(), event.getState(),
				level.getBlockEntity(event.getPos()));
		}
	}
}
