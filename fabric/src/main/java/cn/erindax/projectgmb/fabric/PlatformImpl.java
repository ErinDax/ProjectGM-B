package cn.erindax.projectgmb.fabric;

import cn.erindax.projectgmb.item.KeyItem;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.function.BiFunction;

public final class PlatformImpl {
	private PlatformImpl() {
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	public static Path gameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	public static GameRules.Key<GameRules.BooleanValue> registerBooleanRule(String name, GameRules.Category category,
			boolean defaultValue) {
		return GameRuleRegistry.register(name, category, GameRuleFactory.createBooleanRule(defaultValue));
	}

	public static CreativeModeTab.Builder creativeTab() {
		return FabricItemGroup.builder();
	}

	public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
			Block... blocks) {
		return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
	}

	public static Item keyItem(Item.Properties properties) {
		return new KeyItem(properties);
	}
}
