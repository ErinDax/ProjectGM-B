package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.item.KeyItem;
import cn.erindax.projectgmb.item.LockItem;
import cn.erindax.projectgmb.item.ModComponents;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.lock.net.KeySkinListPayload;
import cn.erindax.projectgmb.lock.net.KeySkinSelectPayload;
import cn.erindax.projectgmb.lock.net.LockOwnerUpdatePayload;
import cn.erindax.projectgmb.lock.net.LockPasswordPayload;
import cn.erindax.projectgmb.lock.net.LockScreenPayload;
import cn.erindax.projectgmb.lock.net.LockSyncPayload;
import cn.erindax.projectgmb.lock.net.LockUpdatePayload;
import cn.erindax.projectgmb.skin.TexturePayload;
import cn.erindax.projectgmb.skin.TextureStore;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LockHandler {

	private static final double MAX_REACH_SQR = 64.0;
	private static final int MAX_PASSWORD_LENGTH = 32;

	private LockHandler() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(TexturePayload.TYPE, TexturePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(LockSyncPayload.TYPE, LockSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(LockUpdatePayload.TYPE, LockUpdatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(LockScreenPayload.TYPE, LockScreenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(KeySkinListPayload.TYPE, KeySkinListPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(LockPasswordPayload.TYPE, LockPasswordPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(LockOwnerUpdatePayload.TYPE, LockOwnerUpdatePayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(KeySkinSelectPayload.TYPE, KeySkinSelectPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(LockPasswordPayload.TYPE,
			(payload, context) -> onPassword(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(LockOwnerUpdatePayload.TYPE,
			(payload, context) -> onOwnerUpdate(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(KeySkinSelectPayload.TYPE,
			(payload, context) -> onKeySkinSelect(context.player(), payload));

		UseBlockCallback.EVENT.register(LockHandler::onUseBlock);
		PlayerBlockBreakEvents.BEFORE.register(LockHandler::onBeforeBreak);
		PlayerBlockBreakEvents.AFTER.register(LockHandler::onAfterBreak);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Locks.sendAll(handler.player);
			TextureStore.KEYS.sendAllTo(handler.player);
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> Locks.sendAll(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Locks.sendAll(newPlayer));
	}

	private static InteractionResult onUseBlock(Player player, net.minecraft.world.level.Level world,
			InteractionHand hand, BlockHitResult hit) {
		if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!Locks.isLockable(level, pos, state)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		Locks.Found found = Locks.find(level, pos, state);

		if (found == null) {
			if (held.getItem() instanceof LockItem lockItem) {
				beginSetup(serverPlayer, pos, state, lockItem.getType());
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		}

		LockData data = found.data();
		if (Locks.isOwner(player, data)) {
			if (player.isShiftKeyDown() || held.getItem() instanceof LockItem) {
				openOwnerScreen(serverPlayer, found);
				Locks.resync(serverPlayer, level, pos, state);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		}
		if (data.isUnlockedFor(player.getUUID())) {
			return InteractionResult.PASS;
		}
		if (held.getItem() instanceof LockItem) {
			deny(serverPlayer, level, pos, state, Component.translatable("lock.projectgm_b.already_locked", data.ownerName()));
			return InteractionResult.FAIL;
		}
		boolean skipsBlockUse = player.isSecondaryUseActive()
			&& (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty());
		if (skipsBlockUse) {
			return InteractionResult.PASS;
		}

		return switch (data.type()) {
			case KEY -> {
				if (data.matchesKey(held) || data.matchesKey(player.getItemInHand(otherHand(hand)))) {
					if (data.mode() == LockMode.SINGLE) {
						data.markUnlocked(player.getUUID());
						Locks.markDirty(level);
						serverPlayer.displayClientMessage(Component.translatable("lock.projectgm_b.unlocked_permanent"), true);
					}
					yield InteractionResult.PASS;
				}
				deny(serverPlayer, level, pos, state, Component.translatable("lock.projectgm_b.need_key"));
				yield InteractionResult.FAIL;
			}
			case PASSWORD -> {
				ServerPlayNetworking.send(serverPlayer, new LockScreenPayload(found.pos(),
					LockScreenPayload.Kind.UNLOCK_PASSWORD, data.type().ordinal(), data.mode().ordinal()));
				Locks.resync(serverPlayer, level, pos, state);
				yield InteractionResult.FAIL;
			}
		};
	}

	private static void beginSetup(ServerPlayer player, BlockPos pos, BlockState state, LockType type) {
		BlockPos canonical = Locks.canonical(pos, state);
		switch (type) {
			case PASSWORD -> ServerPlayNetworking.send(player, new LockScreenPayload(canonical,
				LockScreenPayload.Kind.SETUP_PASSWORD, type.ordinal(), LockMode.EVERY.ordinal()));
			case KEY -> KeyPickMenu.open(player, canonical);
		}
		Locks.resync(player, player.serverLevel(), pos, state);
	}

	private static void openOwnerScreen(ServerPlayer player, Locks.Found found) {
		ServerPlayNetworking.send(player, new LockScreenPayload(found.pos(), LockScreenPayload.Kind.OWNER,
			found.data().type().ordinal(), found.data().mode().ordinal()));
	}

	private static void deny(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, Component message) {
		player.displayClientMessage(message, true);
		Locks.resync(player, level, pos, state);
	}

	private static boolean onBeforeBreak(net.minecraft.world.level.Level world, Player player, BlockPos pos,
			BlockState state, @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (!(world instanceof ServerLevel level)) {
			return true;
		}
		Locks.Found found = Locks.find(level, pos, state);
		if (found == null || Locks.isOwner(player, found.data())) {
			return true;
		}
		player.displayClientMessage(Component.translatable("lock.projectgm_b.cannot_break", found.data().ownerName()), true);
		return false;
	}

	private static void onAfterBreak(net.minecraft.world.level.Level world, Player player, BlockPos pos,
			BlockState state, @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}
		LockState locks = LockState.get(level);
		for (BlockPos candidate : Locks.group(pos, state)) {
			if (locks.get(candidate) != null) {
				LockData removed = Locks.remove(level, candidate);
				if (removed != null) {
					player.getInventory().placeItemBackInInventory(lockItemFor(removed.type()));
				}
			}
		}
	}

	@Nullable
	private static Locks.Found validTarget(ServerPlayer player, BlockPos pos) {
		if (player.blockPosition().distSqr(pos) > MAX_REACH_SQR) {
			return null;
		}
		ServerLevel level = player.serverLevel();
		BlockState state = level.getBlockState(pos);
		if (!Locks.isLockable(level, pos, state)) {
			return null;
		}
		return Locks.find(level, pos, state);
	}

	private static void onPassword(ServerPlayer player, LockPasswordPayload payload) {
		String password = payload.password().strip();
		if (password.isEmpty() || password.length() > MAX_PASSWORD_LENGTH) {
			player.displayClientMessage(Component.translatable("lock.projectgm_b.password_invalid"), true);
			return;
		}
		BlockPos pos = payload.pos();
		ServerLevel level = player.serverLevel();
		BlockState state = level.getBlockState(pos);
		if (payload.setup()) {
			if (player.blockPosition().distSqr(pos) > MAX_REACH_SQR || !Locks.isLockable(level, pos, state)
					|| Locks.find(level, pos, state) != null) {
				return;
			}
			if (!consumeLockItem(player, LockType.PASSWORD)) {
				player.displayClientMessage(Component.translatable("lock.projectgm_b.no_lock_item"), true);
				return;
			}
			LockData data = new LockData(LockType.PASSWORD, player.getUUID(), player.getGameProfile().getName());
			data.setPasswordHash(Locks.hash(password));
			Locks.put(level, Locks.canonical(pos, state), data);
			player.displayClientMessage(Component.translatable("lock.projectgm_b.locked_password"), true);
			return;
		}

		Locks.Found found = validTarget(player, pos);
		if (found == null || found.data().type() != LockType.PASSWORD) {
			return;
		}
		if (!found.data().matchesPassword(password)) {
			player.displayClientMessage(Component.translatable("lock.projectgm_b.password_wrong"), true);
			return;
		}
		if (found.data().mode() == LockMode.SINGLE) {
			found.data().markUnlocked(player.getUUID());
			Locks.markDirty(level);
			player.displayClientMessage(Component.translatable("lock.projectgm_b.unlocked_permanent"), true);
		}
		state.useWithoutItem(level, player, new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
	}

	static void finishKeySetup(ServerPlayer player, BlockPos pos, ItemStack keyStack) {
		ServerLevel level = player.serverLevel();
		BlockState state = level.getBlockState(pos);
		if (player.blockPosition().distSqr(pos) > MAX_REACH_SQR || !Locks.isLockable(level, pos, state)
				|| Locks.find(level, pos, state) != null) {
			return;
		}
		if (!consumeLockItem(player, LockType.KEY)) {
			player.displayClientMessage(Component.translatable("lock.projectgm_b.no_lock_item"), true);
			return;
		}
		LockData data = new LockData(LockType.KEY, player.getUUID(), player.getGameProfile().getName());
		data.setKey(keyStack);
		Locks.put(level, Locks.canonical(pos, state), data);
		player.displayClientMessage(Component.translatable("lock.projectgm_b.locked_key", keyStack.getHoverName()), true);
	}

	private static void onOwnerUpdate(ServerPlayer player, LockOwnerUpdatePayload payload) {
		Locks.Found found = validTarget(player, payload.pos());
		if (found == null || !Locks.isOwner(player, found.data())) {
			return;
		}
		ServerLevel level = player.serverLevel();
		if (payload.remove()) {
			LockData removed = Locks.remove(level, found.pos());
			if (removed != null) {
				player.getInventory().placeItemBackInInventory(lockItemFor(removed.type()));
				player.displayClientMessage(Component.translatable("lock.projectgm_b.removed"), true);
			}
			return;
		}
		found.data().setMode(LockMode.byIndex(payload.mode()));
		Locks.markDirty(level);
		player.displayClientMessage(Component.translatable("lock.projectgm_b.saved"), true);
	}

	public static void openKeySkinPicker(ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!player.hasPermissions(2) || !(held.getItem() instanceof KeyItem)) {
			return;
		}
		List<String> skins = TextureStore.KEYS.listAvailable();
		for (String skin : skins) {
			TextureStore.KEYS.sendTo(player, skin);
		}
		ServerPlayNetworking.send(player, new KeySkinListPayload(hand == InteractionHand.MAIN_HAND,
			KeyItem.skinOf(held), skins));
	}

	private static void onKeySkinSelect(ServerPlayer player, KeySkinSelectPayload payload) {
		InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack held = player.getItemInHand(hand);
		if (!player.hasPermissions(2) || !(held.getItem() instanceof KeyItem)) {
			return;
		}
		String skin = payload.skin();
		if (skin.isEmpty()) {
			held.remove(ModComponents.KEY_SKIN);
			return;
		}
		if (TextureStore.KEYS.load(skin, true) == null) {
			player.displayClientMessage(Component.translatable("lock.projectgm_b.key_skin_missing", skin), true);
			return;
		}
		held.set(ModComponents.KEY_SKIN, skin);
		TextureStore.KEYS.sendToAll(player.server.getPlayerList().getPlayers(), skin);
	}

	private static boolean consumeLockItem(ServerPlayer player, LockType type) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof LockItem lock && lock.getType() == type) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private static ItemStack lockItemFor(LockType type) {
		return new ItemStack(type == LockType.PASSWORD ? ModItems.PASSWORD_LOCK : ModItems.KEY_LOCK);
	}

	private static InteractionHand otherHand(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}
}
