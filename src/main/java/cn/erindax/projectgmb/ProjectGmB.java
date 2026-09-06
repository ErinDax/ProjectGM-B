package cn.erindax.projectgmb;

import cn.erindax.projectgmb.lock.DoorLocks;
import cn.erindax.projectgmb.lock.LockNames;
import cn.erindax.projectgmb.network.ModNetwork;
import cn.erindax.projectgmb.vanish.OpVanish;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.FlowerPotBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectGmB implements ModInitializer {
	public static final String MOD_ID = "projectgm_b";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final GameRules.Key<GameRules.BooleanValue> ADVENTURE_FLOWER_POT = GameRuleRegistry.register(
		"adventureFlowerPot",
		GameRules.Category.PLAYER,
		GameRuleFactory.createBooleanRule(false)
	);

	@Override
	public void onInitialize() {
		LockNames.reload();
		ModNetwork.register();
		DoorLocks.register();
		ServerTickEvents.END_SERVER_TICK.register(OpVanish::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> OpVanish.clear());
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof FlowerPotBlock)) {
				return InteractionResult.PASS;
			}
			return lockAdventureFlowerPot(player) ? InteractionResult.FAIL : InteractionResult.PASS;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!isProtectedDecoration(entity)) {
				return InteractionResult.PASS;
			}
			if (player.isSpectator() || player.getAbilities().mayBuild) {
				return InteractionResult.PASS;
			}
			return InteractionResult.FAIL;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("projectgmb")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("reload")
					.executes(ctx -> {
						LockNames.reload();
						ctx.getSource().sendSuccess(() -> Component.translatable("commands.projectgm_b.reload"), true);
						return 1;
					})))
		);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success) {
				LockNames.reload();
			}
		});
		LOGGER.info("ProjectGM-b initialized!");
	}

	private static boolean isProtectedDecoration(Entity entity) {
		if (entity instanceof ItemFrame frame) {
			return frame.getItem().isEmpty();
		}
		return entity instanceof HangingEntity && !(entity instanceof LeashFenceKnotEntity);
	}

	public static boolean lockAdventureFlowerPot(Player player) {
		return !player.isSpectator()
			&& !player.getAbilities().mayBuild
			&& !player.level().getGameRules().getBoolean(ADVENTURE_FLOWER_POT);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
