package cn.erindax.projectgmb.client.render;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PatrollerModelLayers {

	public static final ModelLayerLocation WIDE = layer("main");
	public static final ModelLayerLocation SLIM = layer("slim");
	public static final ModelLayerLocation INNER_ARMOR = layer("inner_armor");
	public static final ModelLayerLocation OUTER_ARMOR = layer("outer_armor");

	private PatrollerModelLayers() {
	}

	public static void register(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> registrar) {
		registrar.accept(WIDE,
			() -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
		registrar.accept(SLIM,
			() -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64));
		registrar.accept(INNER_ARMOR,
			() -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(new CubeDeformation(0.5F)), 64, 32));
		registrar.accept(OUTER_ARMOR,
			() -> LayerDefinition.create(HumanoidArmorModel.createBodyLayer(new CubeDeformation(1.0F)), 64, 32));
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(ProjectGmB.id("patroller"), name);
	}
}
