package cn.erindax.projectgmb.forge;

import cn.erindax.projectgmb.ModGameRules;
import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.block.ModBlockEntities;
import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.entity.PatrollerEntity;
import cn.erindax.projectgmb.forge.client.ProjectGmBForgeClient;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.platform.Event;
import cn.erindax.projectgmb.platform.ModEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Consumer;

@Mod(ProjectGmB.MOD_ID)
public final class ProjectGmBForge {
	public ProjectGmBForge(FMLJavaModLoadingContext context) {
		IEventBus modBus = context.getModEventBus();
		ProjectGmB.init();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ProjectGmBForgeClient.init(modBus);
		}
		PlatformNetworkImpl.register();
		modBus.addListener(ProjectGmBForge::registerContent);
		modBus.addListener(ProjectGmBForge::buildCreativeTabs);
		modBus.addListener((EntityAttributeCreationEvent event) ->
			event.put(ModEntities.PATROLLER, PatrollerEntity.createAttributes().build()));
		MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
			Event.post(ModEvents.REGISTER_COMMANDS, event.getDispatcher()));
		MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> Event.post(ModEvents.SERVER_STARTED, event.getServer()));
		MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Event.post(ModEvents.SERVER_STOPPED, event.getServer()));
		MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null) {
				Event.post(ModEvents.DATA_PACK_RELOADED, event.getPlayerList().getServer());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				Event.post(ModEvents.SERVER_TICK, event.getServer());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
				Event.post(ModEvents.LEVEL_TICK, level);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> postPlayer(ModEvents.PLAYER_JOIN, event));
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> postPlayer(ModEvents.PLAYER_LEAVE, event));
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) ->
			postPlayer(ModEvents.PLAYER_CHANGED_DIMENSION, event));
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> postPlayer(ModEvents.PLAYER_RESPAWN, event));
		MinecraftForge.EVENT_BUS.addListener(ProjectGmBForge::onUseBlock);
		MinecraftForge.EVENT_BUS.addListener(ProjectGmBForge::onAttackEntity);
		MinecraftForge.EVENT_BUS.addListener(ProjectGmBForge::onBeforeBreak);
	}

	private static void registerContent(RegisterEvent event) {
		ResourceKey<? extends Registry<?>> registry = event.getRegistryKey();
		if (registry.equals(Registries.BLOCK)) {
			ModGameRules.init();
			ModBlocks.init();
		} else if (registry.equals(Registries.ITEM)) {
			ModItems.init();
		} else if (registry.equals(Registries.BLOCK_ENTITY_TYPE)) {
			ModBlockEntities.init();
		} else if (registry.equals(Registries.ENTITY_TYPE)) {
			ModEntities.init();
		}
	}

	private static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey().equals(CreativeModeTabs.OP_BLOCKS)) {
			event.accept(ModItems.BARRIER_PANEL);
		}
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
}
