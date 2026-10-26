package cn.erindax.projectgmb;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.function.BiFunction;

public final class Platform {
	private Platform() {
	}

	@ExpectPlatform
	public static Path configDir() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static Path gameDir() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static GameRules.Key<GameRules.BooleanValue> registerBooleanRule(String name, GameRules.Category category,
			boolean defaultValue) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static CreativeModeTab.Builder creativeTab() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
			Block... blocks) {
		throw new AssertionError();
	}
}
