package cn.erindax.projectgmb.forge;

import cn.erindax.projectgmb.forge.client.KeyItemExtensions;
import cn.erindax.projectgmb.item.KeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class PlatformImpl {
	private PlatformImpl() {
	}

	public static Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public static Path gameDir() {
		return FMLPaths.GAMEDIR.get();
	}

	public static GameRules.Key<GameRules.BooleanValue> registerBooleanRule(String name, GameRules.Category category,
			boolean defaultValue) {
		return GameRules.register(name, category, GameRules.BooleanValue.create(defaultValue));
	}

	public static CreativeModeTab.Builder creativeTab() {
		return CreativeModeTab.builder();
	}

	public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory,
			Block... blocks) {
		return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
	}

	public static Item keyItem(Item.Properties properties) {
		return new KeyItem(properties) {
			@Override
			public void initializeClient(Consumer<IClientItemExtensions> consumer) {
				consumer.accept(new KeyItemExtensions());
			}
		};
	}
}
