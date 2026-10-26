package cn.erindax.projectgmb.block;

import cn.erindax.projectgmb.Platform;
import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<DanceBlockEntity> DANCE_BLOCK = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE, ProjectGmB.id("dance_block"),
		Platform.blockEntityType(DanceBlockEntity::new, ModBlocks.DANCE_BLOCK));

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
