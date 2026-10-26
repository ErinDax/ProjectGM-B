package cn.erindax.projectgmb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DanceBlockEntity extends BlockEntity {

	private static final String DISGUISE = "disguise";
	private static final String DISGUISED = "disguised";

	@Nullable
	private BlockState disguise;

	public DanceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DANCE_BLOCK, pos, state);
	}

	@Nullable
	public BlockState disguise() {
		return disguise;
	}

	public void setDisguise(@Nullable BlockState state) {
		disguise = state;
		setChanged();
		if (level != null) {
			BlockState self = getBlockState();
			level.sendBlockUpdated(worldPosition, self, self, Block.UPDATE_ALL);
			level.getChunkSource().getLightEngine().checkBlock(worldPosition);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean(DISGUISED, disguise != null);
		if (disguise != null) {
			tag.put(DISGUISE, NbtUtils.writeBlockState(disguise));
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		BlockState loaded = null;
		if (tag.contains(DISGUISE, Tag.TAG_COMPOUND)) {
			HolderGetter<Block> blocks = level != null ? level.holderLookup(Registries.BLOCK)
				: BuiltInRegistries.BLOCK.asLookup();
			BlockState state = NbtUtils.readBlockState(blocks, tag.getCompound(DISGUISE));
			if (!state.isAir() && !(state.getBlock() instanceof DanceBlock)) {
				loaded = state;
			}
		}
		boolean changed = !Objects.equals(loaded, disguise);
		disguise = loaded;
		if (changed && level != null && level.isClientSide()) {
			BlockState self = getBlockState();
			level.sendBlockUpdated(worldPosition, self, self, Block.UPDATE_IMMEDIATE);
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
