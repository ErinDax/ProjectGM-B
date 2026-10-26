package cn.erindax.projectgmb.neoforge.client;

import cn.erindax.projectgmb.client.render.KeyItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class KeyItemExtensions implements IClientItemExtensions {
	@Nullable
	private BlockEntityWithoutLevelRenderer renderer;

	@Override
	public BlockEntityWithoutLevelRenderer getCustomRenderer() {
		if (renderer == null) {
			Minecraft minecraft = Minecraft.getInstance();
			renderer = new Renderer(minecraft);
		}
		return renderer;
	}

	private static final class Renderer extends BlockEntityWithoutLevelRenderer {
		private final KeyItemRenderer key = new KeyItemRenderer();

		private Renderer(Minecraft minecraft) {
			super(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
		}

		@Override
		public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
				MultiBufferSource buffers, int light, int overlay) {
			key.render(stack, context, poseStack, buffers, light, overlay);
		}
	}
}
