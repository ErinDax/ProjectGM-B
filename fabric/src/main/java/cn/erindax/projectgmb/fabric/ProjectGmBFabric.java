package cn.erindax.projectgmb.fabric;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.entity.PatrollerEntity;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.Event;
import cn.erindax.projectgmb.platform.ModEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.CreativeModeTabs;

public final class ProjectGmBFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		ProjectGmB.registerContent();
		FabricDefaultAttributeRegistry.register(ModEntities.PATROLLER, PatrollerEntity.createAttributes());
		ProjectGmB.init();
		Network.clientbound().forEach(ProjectGmBFabric::registerClientbound);
		Network.serverbound().forEach(ProjectGmBFabric::registerServerbound);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> entries.accept(ModItems.BARRIER_PANEL));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			Event.post(ModEvents.REGISTER_COMMANDS, dispatcher));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> Event.post(ModEvents.SERVER_STARTED, server));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> Event.post(ModEvents.SERVER_STOPPED, server));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success) {
				Event.post(ModEvents.DATA_PACK_RELOADED, server);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> Event.post(ModEvents.SERVER_TICK, server));
		ServerTickEvents.END_WORLD_TICK.register(level -> Event.post(ModEvents.LEVEL_TICK, level));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			Event.post(ModEvents.PLAYER_JOIN, handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			Event.post(ModEvents.PLAYER_LEAVE, handler.player));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
			Event.post(ModEvents.PLAYER_CHANGED_DIMENSION, player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			Event.post(ModEvents.PLAYER_RESPAWN, newPlayer));
		UseBlockCallback.EVENT.register(ModEvents::useBlock);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
			ModEvents.attackEntity(player, level, hand, entity));
		PlayerBlockBreakEvents.BEFORE.register(ModEvents::beforeBreak);
		PlayerBlockBreakEvents.AFTER.register(ModEvents::afterBreak);
	}

	private static <T extends CustomPacketPayload> void registerClientbound(Network.Clientbound<T> entry) {
		PayloadTypeRegistry.playS2C().register(entry.type(), entry.codec());
	}

	private static <T extends CustomPacketPayload> void registerServerbound(Network.Serverbound<T> entry) {
		PayloadTypeRegistry.playC2S().register(entry.type(), entry.codec());
		ServerPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) ->
			entry.handler().accept(payload, context.player()));
	}
}
