package cn.erindax.projectgmb.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class ModEvents {
	public static final Event<Consumer<MinecraftServer>> SERVER_STARTED = new Event<>();
	public static final Event<Consumer<MinecraftServer>> SERVER_STOPPED = new Event<>();
	public static final Event<Consumer<MinecraftServer>> SERVER_TICK = new Event<>();
	public static final Event<Consumer<ServerLevel>> LEVEL_TICK = new Event<>();
	public static final Event<Consumer<MinecraftServer>> DATA_PACK_RELOADED = new Event<>();
	public static final Event<Consumer<ServerPlayer>> PLAYER_JOIN = new Event<>();
	public static final Event<Consumer<ServerPlayer>> PLAYER_LEAVE = new Event<>();
	public static final Event<Consumer<ServerPlayer>> PLAYER_CHANGED_DIMENSION = new Event<>();
	public static final Event<Consumer<ServerPlayer>> PLAYER_RESPAWN = new Event<>();
	public static final Event<Consumer<CommandDispatcher<CommandSourceStack>>> REGISTER_COMMANDS = new Event<>();
	public static final Event<UseBlock> USE_BLOCK = new Event<>();
	public static final Event<AttackEntity> ATTACK_ENTITY = new Event<>();
	public static final Event<BeforeBreak> BEFORE_BREAK = new Event<>();
	public static final Event<AfterBreak> AFTER_BREAK = new Event<>();

	private ModEvents() {
	}

	public static InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		for (UseBlock listener : USE_BLOCK.listeners()) {
			InteractionResult result = listener.use(player, level, hand, hit);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static InteractionResult attackEntity(Player player, Level level, InteractionHand hand, Entity entity) {
		for (AttackEntity listener : ATTACK_ENTITY.listeners()) {
			InteractionResult result = listener.attack(player, level, hand, entity);
			if (result != InteractionResult.PASS) {
				return result;
			}
		}
		return InteractionResult.PASS;
	}

	public static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity) {
		for (BeforeBreak listener : BEFORE_BREAK.listeners()) {
			if (!listener.allow(level, player, pos, state, blockEntity)) {
				return false;
			}
		}
		return true;
	}

	public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity) {
		for (AfterBreak listener : AFTER_BREAK.listeners()) {
			listener.broken(level, player, pos, state, blockEntity);
		}
	}

	@FunctionalInterface
	public interface UseBlock {
		InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit);
	}

	@FunctionalInterface
	public interface AttackEntity {
		InteractionResult attack(Player player, Level level, InteractionHand hand, Entity entity);
	}

	@FunctionalInterface
	public interface BeforeBreak {
		boolean allow(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}

	@FunctionalInterface
	public interface AfterBreak {
		void broken(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
	}
}
