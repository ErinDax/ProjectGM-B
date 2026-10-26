package cn.erindax.projectgmb.entity.net;

import cn.erindax.projectgmb.entity.PatrolRecorder;
import cn.erindax.projectgmb.entity.PatrollerEntity;
import cn.erindax.projectgmb.entity.Patrollers;
import cn.erindax.projectgmb.manage.WandWhitelist;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.skin.TextureStore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PatrollerSettingsHandler {

	private PatrollerSettingsHandler() {
	}

	public static void init() {
		Network.clientbound(PatrollerSettingsPayload.TYPE, PatrollerSettingsPayload.STREAM_CODEC);
		Network.clientbound(PatrollerListPayload.TYPE, PatrollerListPayload.STREAM_CODEC);
		Network.serverbound(PatrollerUpdatePayload.TYPE, PatrollerUpdatePayload.STREAM_CODEC,
			(payload, player) -> update(player, payload));
		Network.serverbound(PatrollerListActionPayload.TYPE, PatrollerListActionPayload.STREAM_CODEC,
			(payload, player) -> listAction(player, payload));
	}

	public static void openSettings(ServerPlayer player, PatrollerEntity patroller, boolean fromList) {
		List<String> skins = TextureStore.SKINS.listAvailable();
		for (String skin : skins) {
			TextureStore.SKINS.sendTo(player, skin);
		}
		BlockPos pos = patroller.blockPosition();
		Network.send(player, new PatrollerSettingsPayload(
			patroller.getId(),
			patroller.getPatrolName(),
			patroller.getSkinName(),
			patroller.isSlim(),
			patroller.getPauseTicks() / 20,
			patroller.getDetectDiameter(),
			patroller.getStareTicks() / 20,
			patroller.isNameVisible(),
			patroller.showsHeldItems(),
			patroller.isPatrolling(),
			patroller.isHostile(),
			patroller.getWaypoints().size(),
			skins,
			fromList,
			patroller.level().dimension().location().toString(),
			pos.getX(), pos.getY(), pos.getZ()));
	}

	public static void openList(ServerPlayer player) {
		List<PatrollerListPayload.Entry> entries = new ArrayList<>();
		Set<String> skins = new HashSet<>();
		for (PatrollerEntity p : Patrollers.all(player.server)) {
			BlockPos pos = p.blockPosition();
			entries.add(new PatrollerListPayload.Entry(p.getPatrolName(), p.level().dimension().location().toString(),
				pos.getX(), pos.getY(), pos.getZ(), p.getWaypoints().size(), p.getSkinName(), p.isPatrolling(),
				p.isHostile()));
			if (!p.getSkinName().isEmpty()) {
				skins.add(p.getSkinName());
			}
		}
		for (String skin : skins) {
			TextureStore.SKINS.sendTo(player, skin);
		}
		entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		Network.send(player, new PatrollerListPayload(entries));
	}

	@Nullable
	private static PatrollerEntity resolveByName(ServerPlayer player, String name) {
		if (!WandWhitelist.canManage(player)) {
			return null;
		}
		PatrollerEntity patroller = Patrollers.findByName(player.server, name);
		if (patroller == null) {
			player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.not_found", name));
		}
		return patroller;
	}

	private static void update(ServerPlayer player, PatrollerUpdatePayload payload) {
		PatrollerEntity patroller = Patrollers.findByName(player.server, payload.originalName());
		if (patroller == null || !WandWhitelist.canManage(player)) {
			return;
		}
		String name = payload.name().trim();
		if (!Patrollers.isValidName(name)) {
			player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.bad_name", name));
			return;
		}
		PatrollerEntity existing = Patrollers.findByName(player.server, name);
		if (existing != null && existing != patroller) {
			player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.name_taken", name));
			return;
		}
		String skin = payload.skin();
		if (!skin.isEmpty() && TextureStore.SKINS.load(skin, true) == null) {
			player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.skin_missing",
				TextureStore.SKINS.directory().resolve(skin + ".png").toString()));
			return;
		}

		patroller.setPatrolName(name);
		patroller.setPauseTicks(Math.max(0, payload.pauseSeconds()) * 20);
		patroller.setDetectDiameter(payload.detectDiameter());
		patroller.setStareTicks(Math.max(0, payload.stareSeconds()) * 20);
		patroller.setNameVisible(payload.nameVisible());
		patroller.setShowHeldItems(payload.showHeldItems());
		patroller.setPatrolling(payload.patrolling());
		patroller.setHostile(payload.aggressive());
		patroller.setSkin(skin, payload.slim());
		if (!skin.isEmpty() && patroller.level() instanceof ServerLevel level) {
			TextureStore.SKINS.sendToAll(level.players(), skin);
		}
		player.displayClientMessage(Component.translatable("commands.projectgm_b.patrol.saved", name), true);
		if (payload.fromList()) {
			openList(player);
		}
	}

	private static void listAction(ServerPlayer player, PatrollerListActionPayload payload) {
		if (!WandWhitelist.canManage(player)) {
			return;
		}
		switch (payload.action()) {
			case REFRESH -> openList(player);
			case EDIT -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					openSettings(player, patroller, true);
				}
			}
			case TELEPORT -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					player.teleportTo((ServerLevel) patroller.level(), patroller.getX(), patroller.getY(),
						patroller.getZ(), player.getYRot(), player.getXRot());
				}
			}
			case REMOVE -> {
				PatrollerEntity patroller = resolveByName(player, payload.name());
				if (patroller != null) {
					remove(player, patroller);
				}
				openList(player);
			}
			case RECORD -> {
				String name = payload.name().trim();
				if (!Patrollers.isValidName(name)) {
					player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.bad_name", name));
					return;
				}
				if (PatrolRecorder.current(player) != null) {
					player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.record_busy",
						PatrolRecorder.current(player).name()));
					return;
				}
				PatrolRecorder.start(player, name);
			}
		}
	}

	private static void remove(ServerPlayer player, PatrollerEntity patroller) {
		String name = patroller.getPatrolName();
		patroller.discard();
		player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.removed", name));
	}
}
