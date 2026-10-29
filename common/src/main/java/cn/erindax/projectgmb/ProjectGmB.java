package cn.erindax.projectgmb;

import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.command.ModCommands;
import cn.erindax.projectgmb.dance.DanceManager;
import cn.erindax.projectgmb.entity.ModEntities;
import cn.erindax.projectgmb.entity.PatrolRecorder;
import cn.erindax.projectgmb.entity.net.PatrollerSettingsHandler;
import cn.erindax.projectgmb.item.ModComponents;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.lock.DoorLocks;
import cn.erindax.projectgmb.lock.LockHandler;
import cn.erindax.projectgmb.lock.LockNames;
import cn.erindax.projectgmb.manage.WandWhitelist;
import cn.erindax.projectgmb.music.MusicHandler;
import cn.erindax.projectgmb.network.ModNetwork;
import cn.erindax.projectgmb.reinforce.Reinforcements;
import cn.erindax.projectgmb.platform.ModEvents;
import cn.erindax.projectgmb.seal.Seals;
import cn.erindax.projectgmb.skin.TextureStore;
import cn.erindax.projectgmb.vanish.OpVanish;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.FlowerPotBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectGmB {
	public static final String MOD_ID = "projectgm_b";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private ProjectGmB() {
	}

	public static void registerContent() {
		ModGameRules.init();
		ModComponents.init();
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
	}

	public static void init() {
		LockNames.reload();
		Seals.init();
		ModNetwork.register();
		DoorLocks.register();
		LockHandler.init();
		MusicHandler.init();
		Reinforcements.init();
		DanceManager.init();
		PatrollerSettingsHandler.init();
		PatrolRecorder.init();
		TextureStore.KEYS.ensureDirectory();
		TextureStore.SKINS.ensureDirectory();
		WandWhitelist.reload();
		ModCommands.register();
		ModEvents.SERVER_TICK.register(OpVanish::tick);
		ModEvents.SERVER_STOPPED.register(server -> OpVanish.clear());
		ModEvents.USE_BLOCK.register((player, world, hand, hit) -> {
			if (!(player instanceof ServerPlayer serverPlayer)
					|| !(world.getBlockState(hit.getBlockPos()).getBlock() instanceof FlowerPotBlock)
					|| !lockAdventureFlowerPot(player)) {
				return InteractionResult.PASS;
			}
			serverPlayer.containerMenu.sendAllDataToRemote();
			return InteractionResult.FAIL;
		});
		ModEvents.ATTACK_ENTITY.register((player, world, hand, entity) -> {
			if (!isProtectedDecoration(entity)) {
				return InteractionResult.PASS;
			}
			if (player.isSpectator() || player.getAbilities().mayBuild) {
				return InteractionResult.PASS;
			}
			return InteractionResult.FAIL;
		});
		ModEvents.DATA_PACK_RELOADED.register(server -> LockNames.reload());
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
			&& !player.level().getGameRules().getBoolean(ModGameRules.ADVENTURE_FLOWER_POT);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
