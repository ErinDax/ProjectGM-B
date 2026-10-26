package cn.erindax.projectgmb.forge.client;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.block.DanceBlock;
import cn.erindax.projectgmb.block.DanceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class DanceBlockModel extends BakedModelWrapper<BakedModel> {
	private static final ResourceLocation BLOCK_ID = ProjectGmB.id("dance_block");
	private static final String INVENTORY = "inventory";
	private static final ModelProperty<BlockState> DISGUISE = new ModelProperty<>();
	private static final ModelProperty<ModelData> DISGUISE_DATA = new ModelProperty<>();

	private DanceBlockModel(BakedModel wrapped) {
		super(wrapped);
	}

	public static void wrap(Map<ResourceLocation, BakedModel> models) {
		models.replaceAll((id, model) -> id instanceof ModelResourceLocation location
				&& BLOCK_ID.getNamespace().equals(location.getNamespace())
				&& BLOCK_ID.getPath().equals(location.getPath())
				&& !INVENTORY.equals(location.getVariant())
			? new DanceBlockModel(model)
			: model);
	}

	public static int tint(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
		BlockState disguise = level == null || pos == null ? null : disguise(level, pos);
		if (disguise == null) {
			return -1;
		}
		return Minecraft.getInstance().getBlockColors().getColor(disguise, level, pos, tintIndex);
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		BlockState disguise = disguise(level, pos);
		if (disguise == null) {
			return super.getModelData(level, pos, state, modelData);
		}
		ModelData inner = model(disguise).getModelData(level, pos, disguise, ModelData.EMPTY);
		return modelData.derive().with(DISGUISE, disguise).with(DISGUISE_DATA, inner).build();
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		BlockState disguise = data.get(DISGUISE);
		if (disguise == null) {
			return super.getRenderTypes(state, rand, data);
		}
		return model(disguise).getRenderTypes(disguise, rand, inner(data));
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
			ModelData data, @Nullable RenderType renderType) {
		BlockState disguise = data.get(DISGUISE);
		if (disguise == null) {
			return super.getQuads(state, side, rand, data, renderType);
		}
		return model(disguise).getQuads(disguise, side, rand, inner(data), renderType);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		BlockState disguise = data.get(DISGUISE);
		if (disguise == null) {
			return super.getParticleIcon(data);
		}
		return model(disguise).getParticleIcon(inner(data));
	}

	@Nullable
	private static BlockState disguise(BlockAndTintGetter level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof DanceBlockEntity dance)) {
			return null;
		}
		BlockState disguise = dance.disguise();
		if (disguise == null || disguise.isAir() || disguise.getBlock() instanceof DanceBlock) {
			return null;
		}
		return disguise;
	}

	private static ModelData inner(ModelData data) {
		ModelData inner = data.get(DISGUISE_DATA);
		return inner == null ? ModelData.EMPTY : inner;
	}

	private static BakedModel model(BlockState state) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
	}
}
