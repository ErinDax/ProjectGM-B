package cn.erindax.projectgmb.block;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

	public static final Block BARRIER_PANEL = register("barrier_panel",
		new BarrierPanelBlock(BlockBehaviour.Properties.of()
			.strength(-1.0F, 3600000.8F)
			.noLootTable()
			.noOcclusion()
			.sound(SoundType.STONE)));

	public static final DanceBlock DANCE_BLOCK = register("dance_block",
		new DanceBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.strength(1.5F, 6.0F)
			.noOcclusion()
			.dynamicShape()
			.sound(SoundType.METAL)));

	public static final BlockEntityType<DanceBlockEntity> DANCE_BLOCK_ENTITY = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, ProjectGmB.id("dance_block"),
		BlockEntityType.Builder.of(DanceBlockEntity::new, DANCE_BLOCK).build(null));

	private ModBlocks() {
	}

	private static <T extends Block> T register(String name, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, ProjectGmB.id(name), block);
	}

	public static void init() {
	}
}
