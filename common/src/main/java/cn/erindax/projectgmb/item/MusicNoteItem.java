package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.music.MusicBlocks;
import cn.erindax.projectgmb.music.MusicHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class MusicNoteItem extends Item {

	public static final int DEFAULT_RANGE = 32;
	public static final int MIN_RANGE = 1;
	public static final int MAX_RANGE = 256;

	public MusicNoteItem(Properties properties) {
		super(properties);
	}

	public static String trackOf(ItemStack stack) {
		return stack.getOrDefault(ModComponents.MUSIC_TRACK, "");
	}

	public static int rangeOf(ItemStack stack) {
		return clampRange(stack.getOrDefault(ModComponents.MUSIC_RANGE, DEFAULT_RANGE));
	}

	public static int clampRange(int range) {
		return Mth.clamp(range, MIN_RANGE, MAX_RANGE);
	}

	public static String displayName(String track) {
		int dot = track.lastIndexOf('.');
		return dot > 0 ? track.substring(0, dot) : track;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (context.getLevel() instanceof ServerLevel level && player instanceof ServerPlayer serverPlayer) {
			MusicBlocks.toggle(serverPlayer, level, context.getClickedPos(), context.getItemInHand());
		}
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSecondaryUseActive()) {
			return InteractionResultHolder.pass(stack);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			MusicHandler.openMenu(serverPlayer, hand);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		String track = trackOf(stack);
		if (track.isEmpty()) {
			tooltip.add(Component.translatable("item.projectgm_b.music_note.no_track").withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.translatable("item.projectgm_b.music_note.track", displayName(track)).withStyle(ChatFormatting.AQUA));
		}
		tooltip.add(Component.translatable("item.projectgm_b.music_note.range", rangeOf(stack)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.projectgm_b.music_note.tooltip").withStyle(ChatFormatting.DARK_GRAY));
	}
}
