package cn.erindax.projectgmb.client.render;

import cn.erindax.projectgmb.entity.PatrollerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class PatrollerRenderer extends MobRenderer<PatrollerEntity, PlayerModel<PatrollerEntity>> {

	private final PlayerModel<PatrollerEntity> wideModel;
	private final PlayerModel<PatrollerEntity> slimModel;

	public PatrollerRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(PatrollerModelLayers.WIDE), false), 0.5F);
		wideModel = model;
		slimModel = new PlayerModel<>(context.bakeLayer(PatrollerModelLayers.SLIM), true);
		addLayer(new HumanoidArmorLayer<>(this,
			new HumanoidArmorModel<>(context.bakeLayer(PatrollerModelLayers.INNER_ARMOR)),
			new HumanoidArmorModel<>(context.bakeLayer(PatrollerModelLayers.OUTER_ARMOR)),
			context.getModelManager()));
		addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffer, int light, PatrollerEntity entity,
					float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
					float headPitch) {
				if (entity.showsHeldItems()) {
					super.render(poseStack, buffer, light, entity, limbSwing, limbSwingAmount, partialTick,
						ageInTicks, netHeadYaw, headPitch);
				}
			}
		});
	}

	@Override
	public void render(PatrollerEntity entity, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffer, int light) {
		model = entity.isSlim() ? slimModel : wideModel;
		boolean held = entity.showsHeldItems();
		model.rightArmPose = held && !entity.getMainHandItem().isEmpty()
			? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
		model.leftArmPose = held && !entity.getOffhandItem().isEmpty()
			? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
		super.render(entity, yaw, partialTick, poseStack, buffer, light);
	}

	@Override
	public ResourceLocation getTextureLocation(PatrollerEntity entity) {
		return RemoteTextures.skin(entity.getSkinName());
	}
}
