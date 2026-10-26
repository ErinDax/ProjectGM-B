package cn.erindax.projectgmb.entity;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {

	public static final EntityType<PatrollerEntity> PATROLLER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
		ProjectGmB.id("patroller"),
		EntityType.Builder.of(PatrollerEntity::new, MobCategory.MISC)
			.sized(0.6F, 1.8F)
			.clientTrackingRange(10)
			.build("patroller"));

	private ModEntities() {
	}

	public static void init() {
	}
}
