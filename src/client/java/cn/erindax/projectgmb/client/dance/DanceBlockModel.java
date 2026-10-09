package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.block.DanceBlock;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class DanceBlockModel extends ForwardingBakedModel {

	private static final ResourceLocation BLOCK_ID = ProjectGmB.id("dance_block");
	private static final String INVENTORY = "inventory";

	private DanceBlockModel(BakedModel wrapped) {
		super(wrapped);
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyModelAfterBake().register((model, modelContext) -> {
			ModelResourceLocation id = modelContext.topLevelId();
			if (model != null && id != null && BLOCK_ID.equals(id.id()) && !INVENTORY.equals(id.variant())) {
				return new DanceBlockModel(model);
			}
			return model;
		}));
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
			Supplier<RandomSource> randomSupplier, RenderContext context) {
		BlockState disguise = disguise(blockView, pos);
		if (disguise == null) {
			super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		BakedModel model = minecraft.getBlockRenderer().getBlockModel(disguise);
		BlockColors colors = minecraft.getBlockColors();
		BlendMode blend = BlendMode.fromRenderLayer(ItemBlockRenderTypes.getChunkRenderType(disguise));
		Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		MaterialFinder finder = renderer == null ? null : renderer.materialFinder();
		context.pushTransform(quad -> {
			if (finder != null) {
				quad.material(finder.copyFrom(quad.material()).blendMode(blend).find());
			}
			int tintIndex = quad.colorIndex();
			if (tintIndex >= 0) {
				int tint = colors.getColor(disguise, blockView, pos, tintIndex);
				if (tint != -1) {
					for (int vertex = 0; vertex < 4; vertex++) {
						quad.color(vertex, multiply(quad.color(vertex), tint));
					}
				}
				quad.colorIndex(-1);
			}
			return true;
		});
		model.emitBlockQuads(blockView, disguise, pos, randomSupplier, context);
		context.popTransform();
	}

	@Nullable
	private static BlockState disguise(BlockAndTintGetter blockView, BlockPos pos) {
		if (blockView.getBlockEntityRenderData(pos) instanceof BlockState disguise && !disguise.isAir()
				&& !(disguise.getBlock() instanceof DanceBlock)) {
			return disguise;
		}
		return null;
	}

	private static int multiply(int argb, int rgb) {
		int alpha = argb >>> 24;
		int red = ((argb >> 16) & 0xFF) * ((rgb >> 16) & 0xFF) / 255;
		int green = ((argb >> 8) & 0xFF) * ((rgb >> 8) & 0xFF) / 255;
		int blue = (argb & 0xFF) * (rgb & 0xFF) / 255;
		return alpha << 24 | red << 16 | green << 8 | blue;
	}
}
